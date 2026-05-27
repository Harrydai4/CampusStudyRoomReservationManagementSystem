package com.scau.campusstudyroomreservationmanagementsystem.service;

import com.scau.campusstudyroomreservationmanagementsystem.config.JwtService;
import com.scau.campusstudyroomreservationmanagementsystem.support.BusinessException;
import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import com.scau.campusstudyroomreservationmanagementsystem.support.SqlFragments;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AppService {
    /** 预约开始前可签到分钟数（与前端提示一致） */
    private static final int CHECKIN_EARLY_MINUTES = 15;
    /** 预约开始后可签到分钟数；亦用于禁止预约已过期超过该分钟数的时段 */
    private static final int CHECKIN_LATE_MINUTES = 15;
    private static final int CREDIT_SCORE_MAX = 500;
    /** 学生主动取消预约扣分 */
    private static final int CREDIT_CANCEL_PENALTY = -50;
    private static final DateTimeFormatter CHECKIN_WINDOW_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AppService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(Map<String, Object> req) {
        String studentNo = text(req, "studentNo", "username");
        String password = text(req, "password");
        if (studentNo.length() < 10 || password.length() < 6) {
            throw new BusinessException(400, "学号或密码格式不正确");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                studentNo, passwordEncoder.encode(password), "STUDENT", "PENDING", now, now);
        Long userId = jdbc.queryForObject("select id from user_account where username=?", Long.class, studentNo);
        jdbc.update("""
                insert into student_profile(user_id,student_no,name,gender,college,major,grade,phone,email,material_url,audit_status,credit_score,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                userId, studentNo, text(req, "name"), text(req, "gender", "男"), text(req, "college"),
                text(req, "major"), text(req, "grade"), text(req, "phone"), text(req, "email"),
                text(req, "materialUrl", "/uploads/material/register.pdf"), "PENDING", 300, now, now);
        return Map.of("registerId", userId, "auditStatus", "PENDING");
    }

    public Map<String, Object> loginStudent(Map<String, Object> req) {
        String username = text(req, "username", "studentNo");
        String password = text(req, "password");
        Map<String, Object> account = one("select * from user_account where username=? and role='STUDENT'", username);
        if (account == null || !passwordEncoder.matches(password, String.valueOf(account.get("password_hash")))) {
            throw new BusinessException(401, "账号或密码错误");
        }
        String status = String.valueOf(account.get("status"));
        if ("PENDING".equals(status)) {
            throw new BusinessException(403, "注册资料待审核，请耐心等待");
        }
        if ("DISABLED".equals(status)) {
            throw new BusinessException(403, "账号已禁用，请联系管理员");
        }
        if ("BLACKLIST".equals(status)) {
            throw new BusinessException(403, "账号处于黑名单，请联系管理员解除");
        }
        jdbc.update("update user_account set last_login_at=?,updated_at=? where id=?", LocalDateTime.now(), LocalDateTime.now(), account.get("id"));
        Map<String, Object> profile = one("select * from student_profile where user_id=?", account.get("id"));
        CurrentUser user = new CurrentUser(num(account.get("id")), username, "STUDENT", String.valueOf(profile.get("name")));
        return Map.of("token", jwtService.createToken(user), "userInfo", studentInfo(user));
    }

    public Map<String, Object> loginAdmin(Map<String, Object> req) {
        String accountName = text(req, "account", "username");
        String password = text(req, "password");
        Map<String, Object> account = one("select * from admin_account where account=?", accountName);
        if (account == null || !passwordEncoder.matches(password, String.valueOf(account.get("password_hash")))) {
            throw new BusinessException(401, "管理员账号或密码错误");
        }
        if (!"NORMAL".equals(String.valueOf(account.get("status")))) {
            throw new BusinessException(403, "管理员账号已禁用");
        }
        String dbRole = String.valueOf(account.get("role"));
        String role = "SUPER_ADMIN".equals(dbRole) ? "SUPER_ADMIN" : "ADMIN";
        CurrentUser user = new CurrentUser(num(account.get("id")), accountName, role, String.valueOf(account.get("name")));
        return Map.of("token", jwtService.createToken(user), "adminInfo", adminInfo(user));
    }

    public Map<String, Object> studentInfo(CurrentUser user) {
        Map<String, Object> profile = one("""
                select ua.id userId, ua.username, ua.status accountStatus, sp.*
                from user_account ua join student_profile sp on sp.user_id=ua.id
                where ua.id=?
                """, user.id());
        return profile == null ? Map.of() : profile;
    }

    public Map<String, Object> adminInfo(CurrentUser user) {
        Map<String, Object> admin = one("select id,account,name,role,phone,status from admin_account where id=?", user.id());
        return admin == null ? Map.of("id", user.id(), "account", user.username(), "role", user.role()) : admin;
    }

    public List<Map<String, Object>> rooms(CurrentUser user) {
        String sql = """
                select r.*,
                  (select count(*) from seat s where s.room_id=r.id and s.is_seat=1 and s.status='NORMAL') normalSeatCount
                from study_room r
                where (? is null or r.manager_id=? or ?='SUPER_ADMIN')
                order by r.id
                """;
        Long manager = user != null && user.isAdmin() ? user.id() : null;
        String role = user != null ? user.role() : null;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, manager, manager, role);
        rows.forEach(this::decorateRoom);
        return rows;
    }

    public Map<String, Object> room(Long id) {
        Map<String, Object> room = one("select * from study_room where id=?", id);
        if (room == null) {
            throw new BusinessException(404, "自习室不存在");
        }
        decorateRoom(room);
        return room;
    }

    public List<Map<String, Object>> seats(Long roomId) {
        return jdbc.queryForList("select * from seat where room_id=? order by row_no,col_no", roomId);
    }

    public List<Map<String, Object>> availableSeats(Long roomId, String date, String start, String end) {
        LocalDate d = parseDate(date);
        LocalTime st = parseTime(start);
        LocalTime et = parseTime(end);
        validateTimeRange(st, et);
        Map<String, Object> roomInfo = room(roomId);
        validateTimeWithinRoom(st, et, roomInfo);
        List<LocalDateTime> slots = slots(d, st, et);
        List<Map<String, Object>> seats = seats(roomId);
        if (slots.isEmpty()) {
            return seats;
        }
        String marks = String.join(",", Collections.nCopies(slots.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(roomId);
        slots.forEach(params::add);
        List<Map<String, Object>> occupiedRows = jdbc.queryForList("""
                select rs.seat_id from reservation_slot rs
                join seat s on s.id=rs.seat_id
                where s.room_id=? and rs.status='ACTIVE' and rs.slot_start in (%s)
                """.formatted(marks), params.toArray());
        Set<Long> occupied = new HashSet<>();
        occupiedRows.forEach(r -> occupied.add(num(r.get("seat_id"))));
        for (Map<String, Object> seat : seats) {
            boolean usable = bool(seat.get("is_seat")) && "NORMAL".equals(String.valueOf(seat.get("status")));
            seat.put("available", usable && !occupied.contains(num(seat.get("id"))));
            seat.put("reserveState", !usable ? "disabled" : occupied.contains(num(seat.get("id"))) ? "reserved" : "free");
        }
        return seats;
    }

    @Transactional
    public Map<String, Object> createReservation(CurrentUser user, Map<String, Object> req) {
        Long roomId = longText(req, "roomId");
        Long seatId = longText(req, "seatId");
        LocalDate date = parseDate(text(req, "reserveDate", "date"));
        LocalTime start = parseTime(text(req, "startTime"));
        LocalTime end = parseTime(text(req, "endTime"));
        validateTimeRange(start, end);
        if (date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusDays(7))) {
            throw new BusinessException(400, "只能预约今天起 7 天内的座位");
        }
        ensureReservationStartNotTooPast(date, start);
        Map<String, Object> profile = one("select * from student_profile where user_id=?", user.id());
        if (profile == null || !"APPROVED".equals(String.valueOf(profile.get("audit_status")))) {
            throw new BusinessException(403, "学生资料未审核通过，不能预约");
        }
        if (intValue(profile.get("credit_score")) <= 0) {
            throw new BusinessException(403, "信用积分不足，暂不可预约");
        }
        Map<String, Object> room = room(roomId);
        if (!"OPEN".equals(String.valueOf(room.get("status")))) {
            throw new BusinessException(409, "自习室维护中或已关闭");
        }
        validateTimeWithinRoom(start, end, room);
        Map<String, Object> seat = one("select * from seat where id=? and room_id=?", seatId, roomId);
        if (seat == null || !bool(seat.get("is_seat")) || !"NORMAL".equals(String.valueOf(seat.get("status")))) {
            throw new BusinessException(409, "座位不可预约");
        }
        Integer overlap = jdbc.queryForObject("""
                select count(*) from reservation
                where user_id=? and reserve_date=? and status in ('PENDING','USING','TEMP_LEAVE')
                and start_time < ? and end_time > ?
                """, Integer.class, user.id(), Date.valueOf(date), Time.valueOf(end), Time.valueOf(start));
        if (overlap != null && overlap > 0) {
            throw new BusinessException(409, "同一时间段不能重复预约多个座位");
        }
        LocalDateTime now = LocalDateTime.now();
        String no = "R" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now) + String.format("%03d", user.id() % 1000);
        jdbc.update("insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?)",
                no, user.id(), roomId, seatId, Date.valueOf(date), Time.valueOf(start), Time.valueOf(end), "PENDING", now, now);
        Long reservationId = jdbc.queryForObject("select id from reservation where reservation_no=?", Long.class, no);
        try {
            for (LocalDateTime slot : slots(date, start, end)) {
                jdbc.update("insert into reservation_slot(reservation_id,seat_id,slot_start,slot_end,status) values(?,?,?,?,?)",
                        reservationId, seatId, slot, slot.plusMinutes(10), "ACTIVE");
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(409, "该座位当前时段已被预约");
        }
        jdbc.update("insert into notification_message(user_id,title,content,type,related_id,created_at) values(?,?,?,?,?,?)",
                user.id(), "预约成功", "你的座位预约已创建，请按时签到。", "RESERVATION", reservationId, now);
        return reservationDetail(reservationId);
    }

    public List<Map<String, Object>> myReservations(CurrentUser user, String status, Boolean today) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select r.*, sr.name roomName, sr.location, s.seat_no seatNo,
                       tl.leave_time tempLeaveTime, tl.max_leave_minutes maxLeaveMinutes
                from reservation r join study_room sr on sr.id=r.room_id join seat s on s.id=r.seat_id
                left join temp_leave tl on tl.reservation_id=r.id and tl.leave_status='ACTIVE'
                where r.user_id=?
                """);
        params.add(user.id());
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" and r.status=?");
            params.add(status);
        }
        if (Boolean.TRUE.equals(today)) {
            sql.append(" and r.reserve_date=?");
            params.add(Date.valueOf(LocalDate.now()));
        }
        sql.append(" order by r.reserve_date desc,r.start_time desc,r.id desc");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> reservationDetail(Long id) {
        Map<String, Object> row = one("""
                select r.*, sr.name roomName, sr.location, s.seat_no seatNo, sp.name studentName, sp.student_no studentNo
                from reservation r
                join study_room sr on sr.id=r.room_id
                join seat s on s.id=r.seat_id
                join student_profile sp on sp.user_id=r.user_id
                where r.id=?
                """, id);
        if (row == null) {
            throw new BusinessException(404, "预约不存在");
        }
        return row;
    }

    @Transactional
    public void cancelReservation(CurrentUser user, Long id) {
        Map<String, Object> r = reservationDetail(id);
        if (!Objects.equals(num(r.get("user_id")), user.id())) {
            throw new BusinessException(403, "不能取消他人的预约");
        }
        if (!"PENDING".equals(String.valueOf(r.get("status")))) {
            throw new BusinessException(409, "当前预约不能取消");
        }
        jdbc.update("update reservation set status='CANCELLED',cancel_reason='学生主动取消',updated_at=? where id=?", LocalDateTime.now(), id);
        releaseReservationSlots(id);
        changeCredit(user.id(), CREDIT_CANCEL_PENALTY, "USER_CANCEL", "学生主动取消预约", id);
        notifyUser(user.id(), "预约已取消", "你的预约已取消，已扣除 50 信用分。", "RESERVATION", id);
    }

    public Map<String, Object> qrCode(CurrentUser user) {
        List<Map<String, Object>> pending = myReservations(user, "PENDING", false);
        if (pending.isEmpty()) {
            throw new BusinessException(404, "当前没有待签到预约");
        }
        Map<String, Object> profile = studentInfo(user);
        Map<String, Object> reservation = pending.get(0);
        ensureWithinCheckinWindow(reservation);
        Long reservationId = num(reservation.get("id"));
        long now = System.currentTimeMillis();
        LocalDateTime windowStart = checkinWindowStart(reservation);
        LocalDateTime windowEnd = checkinWindowEnd(reservation);
        String payload = String.join(":",
                String.valueOf(user.id()),
                String.valueOf(reservationId),
                String.valueOf(now),
                String.valueOf(profile.get("student_no")),
                String.valueOf(profile.get("name")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qrToken", Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
        result.put("studentNo", profile.get("student_no"));
        result.put("name", profile.get("name"));
        result.put("reservationId", reservationId);
        result.put("reservationNo", reservation.get("reservation_no"));
        result.put("roomName", reservation.get("roomName"));
        result.put("seatNo", reservation.get("seatNo"));
        result.put("reserveDate", reservation.get("reserve_date"));
        result.put("startTime", reservation.get("start_time"));
        result.put("endTime", reservation.get("end_time"));
        result.put("generatedAt", now);
        result.put("expireAt", now + 60_000);
        result.put("expireSeconds", 60);
        result.put("checkinWindowStart", windowStart);
        result.put("checkinWindowEnd", windowEnd);
        return result;
    }

    @Transactional
    public Map<String, Object> checkout(CurrentUser user, Long id) {
        Map<String, Object> r = reservationDetail(id);
        if (!Objects.equals(num(r.get("user_id")), user.id())) {
            throw new BusinessException(403, "不能签退他人的预约");
        }
        if (!"USING".equals(String.valueOf(r.get("status"))) && !"TEMP_LEAVE".equals(String.valueOf(r.get("status")))) {
            throw new BusinessException(409, "当前预约不是使用中");
        }
        if ("TEMP_LEAVE".equals(String.valueOf(r.get("status")))) {
            jdbc.update("update temp_leave set leave_status='RETURNED',return_time=? where reservation_id=? and leave_status='ACTIVE'",
                    LocalDateTime.now(), id);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime signIn = toLocalDateTime(r.get("sign_in_time"));
        int minutes = (int) Math.max(0, Duration.between(signIn, now).toMinutes());
        jdbc.update("update reservation set status='COMPLETED',sign_out_time=?,actual_minutes=?,updated_at=? where id=?",
                now, minutes, now, id);
        jdbc.update("update checkin_record set checkout_time=? where reservation_id=?", now, id);
        releaseReservationSlots(id);
        Map<String, Object> summary = reservationDetail(id);
        summary.put("actualMinutes", minutes);
        return summary;
    }

    public Map<String, Object> credit(CurrentUser user) {
        Map<String, Object> profile = one("select credit_score from student_profile where user_id=?", user.id());
        List<Map<String, Object>> logs = jdbc.queryForList("select * from credit_log where user_id=? order by created_at desc", user.id());
        return Map.of("score", profile == null ? 0 : profile.get("credit_score"), "logs", logs);
    }

    public Map<String, Object> myStudyDuration(CurrentUser user, String period) {
        String normalized = period == null || period.isBlank() ? "week" : period;
        List<Map<String, Object>> rows;
        if ("day".equalsIgnoreCase(normalized)) {
            rows = jdbc.queryForList("""
                    select hour(sign_in_time) label, sum(actual_minutes) minutes
                    from reservation
                    where user_id=? and status in ('COMPLETED','AUTO_CHECKOUT')
                      and reserve_date=current_date()
                    group by hour(sign_in_time)
                    order by hour(sign_in_time)
                    """, user.id());
        } else if ("year".equalsIgnoreCase(normalized)) {
            rows = jdbc.queryForList("""
                    select date_format(reserve_date,'%Y-%m') label, sum(actual_minutes) minutes
                    from reservation
                    where user_id=? and status in ('COMPLETED','AUTO_CHECKOUT')
                      and reserve_date >= date_sub(current_date(), interval 365 day)
                    group by date_format(reserve_date,'%Y-%m')
                    order by label
                    """, user.id());
        } else if ("month".equalsIgnoreCase(normalized)) {
            rows = jdbc.queryForList("""
                    select concat('第', floor((day(reserve_date)-1)/7)+1, '周') label, sum(actual_minutes) minutes
                    from reservation
                    where user_id=? and status in ('COMPLETED','AUTO_CHECKOUT')
                      and reserve_date between date_sub(current_date(), interval 29 day) and current_date()
                    group by floor((day(reserve_date)-1)/7)
                    order by floor((day(reserve_date)-1)/7)
                    """, user.id());
        } else {
            int days = 7;
            rows = jdbc.queryForList("""
                    select reserve_date label, sum(actual_minutes) minutes
                    from reservation
                    where user_id=? and status in ('COMPLETED','AUTO_CHECKOUT')
                      and reserve_date between date_sub(current_date(), interval ? day) and current_date()
                    group by reserve_date
                    order by reserve_date
                    """, user.id(), days - 1);
        }
        int total = rows.stream().mapToInt(r -> intValue(r.get("minutes"))).sum();
        int reservationCount = jdbc.queryForObject("select count(*) from reservation where user_id=?", Integer.class, user.id());
        int checkinCount = jdbc.queryForObject("""
                select count(*) from checkin_record where user_id=? and result='ON_TIME'
                """, Integer.class, user.id());
        int violationCount = jdbc.queryForObject("""
                select count(*) from reservation where user_id=? and status in ('VIOLATED','AUTO_CANCELLED')
                """, Integer.class, user.id());
        return Map.of("period", normalized, "totalMinutes", total, "series", rows,
                "reservationCount", reservationCount, "checkinCount", checkinCount, "violationCount", violationCount);
    }

    public List<Map<String, Object>> announcements() {
        return jdbc.queryForList("select * from announcement where status='PUBLISHED' order by pinned desc,published_at desc,id desc");
    }

    public void readAnnouncement(Long id) {
        jdbc.update("update announcement set view_count=view_count+1 where id=?", id);
    }

    public List<Map<String, Object>> notifications(CurrentUser user) {
        return jdbc.queryForList("select * from notification_message where user_id=? order by created_at desc", user.id());
    }

    public void readNotification(CurrentUser user, Long id) {
        jdbc.update("update notification_message set read_flag=1,read_at=? where id=? and user_id=?", LocalDateTime.now(), id, user.id());
    }

    public void readAllNotifications(CurrentUser user) {
        jdbc.update("update notification_message set read_flag=1,read_at=? where user_id=? and read_flag=0", LocalDateTime.now(), user.id());
    }

    public Map<String, Object> createFeedback(CurrentUser user, Map<String, Object> req) {
        LocalDateTime now = LocalDateTime.now();
        String severity = text(req, "severity", "MEDIUM").toUpperCase(Locale.ROOT);
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(severity)) {
            severity = "MEDIUM";
        }
        jdbc.update("insert into feedback_ticket(user_id,reservation_id,room_id,seat_id,type,severity,content,status,created_at) values(?,?,?,?,?,?,?,?,?)",
                user.id(), nullableLong(req, "reservationId"), nullableLong(req, "roomId"), nullableLong(req, "seatId"),
                text(req, "type", "SUGGESTION"), severity, text(req, "content"), "PENDING", now);
        Long id = jdbc.queryForObject("select last_insert_id()", Long.class);
        return one("select * from feedback_ticket where id=?", id);
    }

    public List<Map<String, Object>> myFeedback(CurrentUser user) {
        return jdbc.queryForList("select * from feedback_ticket where user_id=? order by created_at desc", user.id());
    }

    public void changePassword(CurrentUser user, Map<String, Object> req) {
        String oldPassword = text(req, "oldPassword");
        String newPassword = text(req, "newPassword");
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            throw new BusinessException(400, "请填写原密码和新密码");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(400, "新密码长度需为 6-20 位");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException(400, "新密码需同时包含字母和数字");
        }
        Map<String, Object> account;
        if (user.isStudent()) {
            account = one("select password_hash from user_account where id=?", user.id());
        } else {
            account = one("select password_hash from admin_account where id=?", user.id());
        }
        if (account == null || !passwordEncoder.matches(oldPassword, String.valueOf(account.get("password_hash")))) {
            throw new BusinessException(400, "原密码不正确");
        }
        String hash = passwordEncoder.encode(newPassword);
        LocalDateTime now = LocalDateTime.now();
        if (user.isStudent()) {
            jdbc.update("update user_account set password_hash=?,updated_at=? where id=?", hash, now, user.id());
        } else {
            jdbc.update("update admin_account set password_hash=?,updated_at=? where id=?", hash, now, user.id());
        }
        writeOperationLog(user, "AUTH", "CHANGE_PASSWORD", user.isStudent() ? "STUDENT" : "ADMIN", user.id(), "修改登录密码");
    }

    public Map<String, Object> dashboard(CurrentUser admin) {
        return Map.of("ok", true);
    }

    /** 待签到 / 使用中 / 暂离中的实时预约（签到页） */
    public List<Map<String, Object>> liveReservations(CurrentUser admin) {
        String liveWhere = sqlWhereWithRoomScope(admin, "r", "r.status in ('PENDING','USING','TEMP_LEAVE')", null);
        String liveSql = sqlJoin("""
                select r.id, r.reservation_no reservationNo, r.status, r.reserve_date reserveDate,
                       r.start_time startTime, r.end_time endTime,
                       sp.student_no studentNo, sp.name studentName, sr.name roomName, s.seat_no seatNo
                from reservation r
                join student_profile sp on sp.user_id=r.user_id
                join study_room sr on sr.id=r.room_id
                join seat s on s.id=r.seat_id""", liveWhere,
                "order by r.reserve_date, r.start_time", "limit 30");
        return jdbc.queryForList(liveSql);
    }

    public List<Map<String, Object>> adminUsers(String keyword, String auditStatus) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select ua.id userId, ua.username, ua.status accountStatus, sp.*
                from user_account ua join student_profile sp on sp.user_id=ua.id
                where ua.role='STUDENT'
                """);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" and (ua.username like ? or sp.name like ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (auditStatus != null && !auditStatus.isBlank()) {
            sql.append(" and sp.audit_status=?");
            params.add(auditStatus);
        }
        sql.append(" order by sp.created_at desc");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /** 导出当前筛选条件下的学生用户 CSV（含注册资料字段） */
    public String exportUsersCsv(String keyword, String auditStatus) {
        List<Map<String, Object>> rows = adminUsers(keyword, auditStatus);
        StringBuilder csv = new StringBuilder("学号,姓名,性别,学院,专业,年级,手机,邮箱,信用分,审核状态,账号状态,身份材料,注册时间\n");
        for (Map<String, Object> row : rows) {
            csv.append(csvCell(row.get("student_no"))).append(',')
                    .append(csvCell(row.get("name"))).append(',')
                    .append(csvCell(row.get("gender"))).append(',')
                    .append(csvCell(row.get("college"))).append(',')
                    .append(csvCell(row.get("major"))).append(',')
                    .append(csvCell(row.get("grade"))).append(',')
                    .append(csvCell(row.get("phone"))).append(',')
                    .append(csvCell(row.get("email"))).append(',')
                    .append(csvCell(row.get("credit_score"))).append(',')
                    .append(csvCell(auditStatusLabel(String.valueOf(row.get("audit_status"))))).append(',')
                    .append(csvCell(accountStatusLabel(String.valueOf(row.get("accountStatus"))))).append(',')
                    .append(csvCell(row.get("material_url"))).append(',')
                    .append(csvCell(row.get("created_at"))).append('\n');
        }
        return csv.toString();
    }

    private static String auditStatusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    private static String accountStatusLabel(String status) {
        return switch (status) {
            case "NORMAL" -> "正常";
            case "PENDING" -> "待审核";
            case "DISABLED" -> "已禁用";
            case "BLACKLIST" -> "黑名单";
            default -> status;
        };
    }

    private static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    public void auditUser(CurrentUser admin, Long id, boolean approve, String remark) {
        String audit = approve ? "APPROVED" : "REJECTED";
        String status = approve ? "NORMAL" : "DISABLED";
        jdbc.update("update student_profile set audit_status=?,audit_remark=?,updated_at=? where user_id=?",
                audit, remark, LocalDateTime.now(), id);
        jdbc.update("update user_account set status=?,updated_at=? where id=?", status, LocalDateTime.now(), id);
        writeOperationLog(admin, "USER", approve ? "APPROVE" : "REJECT", "STUDENT", id, remark);
    }

    public void setUserStatus(Long id, String status) {
        jdbc.update("update user_account set status=?,updated_at=? where id=?", status, LocalDateTime.now(), id);
    }

    @Transactional
    public Map<String, Object> saveRoom(CurrentUser admin, Long id, Map<String, Object> req) {
        boolean creating = id == null;
        if (creating && !admin.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可新增自习室");
        }
        Long managerId;
        if (admin.isSuperAdmin()) {
            managerId = nullableLong(req, "managerId");
            if (creating && managerId == null) {
                throw new BusinessException(400, "请选择自习室负责人");
            }
            if (!creating && managerId == null) {
                managerId = num(room(id).get("manager_id"));
            }
        } else {
            if (creating) {
                throw new BusinessException(403, "仅超级管理员可新增自习室");
            }
            if (!Objects.equals(num(room(id).get("manager_id")), admin.id())) {
                throw new BusinessException(403, "无权限修改该自习室");
            }
            managerId = admin.id();
        }
        int rows = Math.max(1, intText(req, "rowCount", 4));
        int cols = Math.max(1, intText(req, "colCount", 6));
        int seats = rows * cols;
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            String code = text(req, "roomCode", "ROOM-" + System.currentTimeMillis());
            jdbc.update("""
                    insert into study_room(room_code,name,location,floor,open_time,close_time,status,manager_id,row_count,col_count,cell_count,seat_count,facilities,layout_image_url,created_at,updated_at)
                    values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, code, text(req, "name"), text(req, "location"), text(req, "floor", "1楼"),
                    text(req, "openTime", "07:00:00"), text(req, "closeTime", "22:30:00"), text(req, "status", "OPEN"),
                    managerId, rows, cols, seats, seats, text(req, "facilities", "空调,WiFi"), text(req, "layoutImageUrl", ""), now, now);
            id = jdbc.queryForObject("select id from study_room where room_code=?", Long.class, code);
            recreateSeats(id, rows, cols, "N");
        } else {
            Map<String, Object> before = room(id);
            int oldRows = intValue(before.get("row_count"));
            int oldCols = intValue(before.get("col_count"));
            jdbc.update("""
                    update study_room set name=?,location=?,floor=?,open_time=?,close_time=?,status=?,manager_id=?,row_count=?,col_count=?,cell_count=?,seat_count=?,facilities=?,layout_image_url=?,updated_at=?
                    where id=?
                    """, text(req, "name"), text(req, "location"), text(req, "floor", "1楼"),
                    text(req, "openTime", "07:00:00"), text(req, "closeTime", "22:30:00"), text(req, "status", "OPEN"),
                    managerId, rows, cols, seats, seats, text(req, "facilities", "空调,WiFi"), text(req, "layoutImageUrl", ""), now, id);
            if (oldRows != rows || oldCols != cols) {
                syncRoomSeatGrid(id, rows, cols, String.valueOf(before.get("room_code")));
            }
        }
        writeOperationLog(admin, "ROOM", creating ? "CREATE" : "UPDATE", "STUDY_ROOM", id, text(req, "name"));
        return room(id);
    }

    public void deleteRoom(CurrentUser admin, Long id) {
        if (!admin.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可删除自习室");
        }
        Map<String, Object> roomInfo = room(id);
        Integer pending = jdbc.queryForObject("select count(*) from reservation where room_id=? and status in ('PENDING','USING','TEMP_LEAVE')", Integer.class, id);
        if (pending != null && pending > 0) {
            throw new BusinessException(409, "该自习室有未完成预约，不能删除");
        }
        jdbc.update("delete from seat where room_id=?", id);
        jdbc.update("delete from study_room where id=?", id);
        writeOperationLog(admin, "ROOM", "DELETE", "STUDY_ROOM", id, String.valueOf(roomInfo.get("name")));
    }

    public void updateSeat(CurrentUser admin, Long id, Map<String, Object> req) {
        Map<String, Object> seat = one("select room_id from seat where id=?", id);
        if (seat == null) {
            throw new BusinessException(404, "座位不存在");
        }
        assertRoomManager(admin, num(seat.get("room_id")));
        jdbc.update("""
                update seat set is_seat=?,cell_category=?,seat_type=?,has_power=?,near_window=?,quiet_zone=?,hot_seat=?,status=?,updated_at=?
                where id=?
                """, intText(req, "isSeat", 1), text(req, "cellCategory", "SEAT"), text(req, "seatType", "普通座位"),
                intText(req, "hasPower", 0), intText(req, "nearWindow", 0), intText(req, "quietZone", 0),
                intText(req, "hotSeat", 0), text(req, "status", "NORMAL"), LocalDateTime.now(), id);
    }

    @Transactional
    public Map<String, Object> addSeat(CurrentUser admin, Long roomId) {
        assertRoomManager(admin, roomId);
        Map<String, Object> roomInfo = room(roomId);
        int rows = intValue(roomInfo.get("row_count"));
        int cols = intValue(roomInfo.get("col_count"));
        Integer existing = jdbc.queryForObject("select count(*) from seat where room_id=?", Integer.class, roomId);
        int count = existing == null ? 0 : existing;
        int nextIndex = count + 1;
        if (nextIndex > rows * cols) {
            cols += 1;
            jdbc.update("update study_room set col_count=?,cell_count=?,seat_count=?,updated_at=? where id=?",
                    cols, rows * cols, rows * cols, LocalDateTime.now(), roomId);
        }
        int rowNo = ((nextIndex - 1) / cols) + 1;
        int colNo = ((nextIndex - 1) % cols) + 1;
        String prefix = String.valueOf(roomInfo.get("room_code")).replaceAll("[^A-Za-z0-9]", "");
        if (prefix.isBlank()) {
            prefix = "S";
        }
        String seatNo = prefix + "-" + String.format("%02d", nextIndex);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                insert into seat(room_id,seat_no,row_no,col_no,is_seat,cell_category,seat_type,has_power,near_window,quiet_zone,hot_seat,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, roomId, seatNo, rowNo, colNo, 1, "SEAT", "普通座位", colNo % 2 == 0 ? 1 : 0,
                colNo == 1 || colNo == cols ? 1 : 0, rowNo <= 2 ? 1 : 0, 0, "NORMAL", now, now);
        Long seatId = jdbc.queryForObject("select id from seat where room_id=? and seat_no=? order by id desc limit 1", Long.class, roomId, seatNo);
        jdbc.update("update study_room set seat_count=(select count(*) from seat where room_id=? and is_seat=1), updated_at=? where id=?",
                roomId, now, roomId);
        writeOperationLog(admin, "SEAT", "CREATE", "SEAT", seatId, seatNo);
        return one("select * from seat where id=?", seatId);
    }

    @Transactional
    public void deleteSeat(CurrentUser admin, Long seatId) {
        Map<String, Object> seat = one("select s.*, sr.manager_id from seat s join study_room sr on sr.id=s.room_id where s.id=?", seatId);
        if (seat == null) {
            throw new BusinessException(404, "座位不存在");
        }
        if (!admin.isSuperAdmin() && !Objects.equals(num(seat.get("manager_id")), admin.id())) {
            throw new BusinessException(403, "无权限删除该座位");
        }
        Integer active = jdbc.queryForObject("""
                select count(*) from reservation r
                where r.seat_id=? and r.status in ('PENDING','USING','TEMP_LEAVE')
                """, Integer.class, seatId);
        if (active != null && active > 0) {
            throw new BusinessException(409, "该座位存在进行中的预约，无法删除");
        }
        Long roomId = num(seat.get("room_id"));
        jdbc.update("delete from seat where id=?", seatId);
        jdbc.update("update study_room set seat_count=(select count(*) from seat where room_id=? and is_seat=1), updated_at=? where id=?",
                roomId, LocalDateTime.now(), roomId);
        writeOperationLog(admin, "SEAT", "DELETE", "SEAT", seatId, String.valueOf(seat.get("seat_no")));
    }

    private void assertRoomManager(CurrentUser admin, Long roomId) {
        Map<String, Object> roomInfo = room(roomId);
        if (!admin.isSuperAdmin() && !Objects.equals(num(roomInfo.get("manager_id")), admin.id())) {
            throw new BusinessException(403, "无权限管理该自习室座位");
        }
    }

    public void batchSeats(CurrentUser admin, Long roomId, Map<String, Object> req) {
        assertRoomManager(admin, roomId);
        jdbc.update("update seat set seat_type=?,has_power=?,near_window=?,quiet_zone=?,hot_seat=?,status=?,updated_at=? where room_id=?",
                text(req, "seatType", "普通座位"), intText(req, "hasPower", 0), intText(req, "nearWindow", 0),
                intText(req, "quietZone", 0), intText(req, "hotSeat", 0), text(req, "status", "NORMAL"), LocalDateTime.now(), roomId);
    }

    public List<Map<String, Object>> adminReservations(CurrentUser admin) {
        String extra = admin.isSuperAdmin() ? "" : sqlJoin("where", "sr.manager_id=" + admin.id());
        String reservationSql = sqlJoin("""
                select r.*, sp.student_no studentNo, sp.name studentName, sr.name roomName, s.seat_no seatNo
                from reservation r
                join student_profile sp on sp.user_id=r.user_id
                join study_room sr on sr.id=r.room_id
                join seat s on s.id=r.seat_id""", extra,
                "order by r.reserve_date desc, r.start_time desc");
        return jdbc.queryForList(reservationSql);
    }

    /** 管理员撤销违约：恢复信用分并将预约标记为已取消 */
    @Transactional
    public Map<String, Object> revokeViolation(CurrentUser admin, Long reservationId, String remark) {
        Map<String, Object> r = reservationDetail(reservationId);
        String status = String.valueOf(r.get("status"));
        if (!"VIOLATED".equals(status) && !"AUTO_CANCELLED".equals(status)) {
            throw new BusinessException(409, "仅违约或超时取消的预约可撤销");
        }
        String cancelReason = String.valueOf(r.get("cancel_reason"));
        if (cancelReason.contains("管理员撤销违约")) {
            throw new BusinessException(409, "该违约记录已撤销");
        }
        Integer revoked = jdbc.queryForObject(
                "select count(*) from credit_log where reservation_id=? and change_type='VIOLATION_REVOKE'",
                Integer.class, reservationId);
        if (revoked != null && revoked > 0) {
            throw new BusinessException(409, "该违约惩罚已撤销");
        }
        Map<String, Object> room = room(num(r.get("room_id")));
        if (!admin.isSuperAdmin() && !Objects.equals(num(room.get("manager_id")), admin.id())) {
            throw new BusinessException(403, "无权限撤销该自习室预约的违约记录");
        }
        Long userId = num(r.get("user_id"));
        List<Map<String, Object>> penalties = jdbc.queryForList("""
                select change_value, change_type from credit_log
                where reservation_id=? and change_value < 0
                  and change_type not in ('USER_CANCEL','INVALID_CHECKIN_REVERT')
                order by created_at asc
                """, reservationId);
        int restore = penalties.stream().mapToInt(p -> Math.abs(intValue(p.get("change_value")))).sum();
        if (restore == 0 && "VIOLATED".equals(status)) {
            restore = 50;
        }
        if (restore > 0) {
            String reason = remark == null || remark.isBlank() ? "管理员撤销违约惩罚" : "管理员撤销违约：" + remark.trim();
            changeCredit(userId, restore, "VIOLATION_REVOKE", reason, reservationId);
        }
        String detail = remark == null || remark.isBlank() ? "管理员撤销违约" : "管理员撤销违约：" + remark.trim();
        jdbc.update("update reservation set status='CANCELLED',cancel_reason=?,updated_at=? where id=?",
                detail, LocalDateTime.now(), reservationId);
        Map<String, Object> account = one("select status from user_account where id=?", userId);
        if (account != null && "BLACKLIST".equals(String.valueOf(account.get("status")))) {
            Map<String, Object> profile = one("select credit_score from student_profile where user_id=?", userId);
            if (profile != null && intValue(profile.get("credit_score")) > 0) {
                jdbc.update("update user_account set status='NORMAL',updated_at=? where id=?", LocalDateTime.now(), userId);
                jdbc.update("update blacklist_record set status='RELEASED',released_at=? where user_id=? and status='ACTIVE'",
                        LocalDateTime.now(), userId);
            }
        }
        writeOperationLog(admin, "RESERVATION", "REVOKE_VIOLATION", "RESERVATION", reservationId,
                detail + (restore > 0 ? "，恢复" + restore + "分" : ""));
        notifyUser(userId, "违约已撤销",
                restore > 0 ? "管理员已撤销你的违约记录，已恢复 " + restore + " 信用分。" : "管理员已撤销你的违约记录。",
                "VIOLATION", reservationId);
        return reservationDetail(reservationId);
    }

    @Transactional
    public Map<String, Object> scanCheckin(CurrentUser admin, Map<String, Object> req) {
        String studentNo = text(req, "studentNo", "").trim();
        String token = text(req, "qrToken", "");
        Long userId = nullableLong(req, "userId");
        Long reservationId = nullableLong(req, "reservationId");
        String checkinMethod = "QR_SCAN";

        if (!studentNo.isBlank()) {
            Map<String, Object> profile = one("""
                    select sp.user_id, sp.student_no, sp.name, sp.audit_status
                    from student_profile sp
                    join user_account ua on ua.id = sp.user_id
                    where sp.student_no=?
                    """, studentNo);
            if (profile == null) {
                throw new BusinessException(404, "学号不存在：" + studentNo);
            }
            if (!"APPROVED".equals(String.valueOf(profile.get("audit_status")))) {
                throw new BusinessException(403, "该学号注册尚未通过审核");
            }
            userId = num(profile.get("user_id"));
            reservationId = null;
            checkinMethod = "STUDENT_NO";
        } else if (!token.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
                String[] parts = decoded.split(":");
                userId = Long.parseLong(parts[0]);
                reservationId = Long.parseLong(parts[1]);
                long issuedAt = Long.parseLong(parts[2]);
                long nowMillis = System.currentTimeMillis();
                if (issuedAt > nowMillis + 5_000 || nowMillis - issuedAt > 60_000) {
                    throw new BusinessException(400, "二维码已过期，请让学生重新生成");
                }
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(400, "二维码无效或已过期");
            }
        }
        if (userId == null && reservationId == null) {
            throw new BusinessException(400, "请输入学生学号");
        }
        Map<String, Object> r = reservationId != null && reservationId > 0
                ? reservationDetail(reservationId)
                : one("""
                        select * from reservation
                        where user_id=? and status='PENDING'
                        order by reserve_date desc, start_time desc, id desc
                        limit 1
                        """, userId);
        if (r == null) {
            throw new BusinessException(404, "该学生当前没有待签到的预约");
        }
        Map<String, Object> room = room(num(r.get("room_id")));
        if (!admin.isSuperAdmin() && !Objects.equals(num(room.get("manager_id")), admin.id())) {
            throw new BusinessException(403, "无权限为该自习室签到，请使用 superadmin 或该自习室管理员账号");
        }
        if (!"PENDING".equals(String.valueOf(r.get("status")))) {
            throw new BusinessException(409, "该预约不能签到");
        }
        ensureWithinCheckinWindow(r);
        LocalDateTime now = LocalDateTime.now();
        Long rid = num(r.get("id"));
        Long uid = num(r.get("user_id"));
        jdbc.update("delete from checkin_record where reservation_id=?", rid);
        jdbc.update("update reservation set status='USING',sign_in_time=?,updated_at=? where id=?", now, now, rid);
        jdbc.update("insert into checkin_record(reservation_id,user_id,admin_id,checkin_method,checkin_time,result) values(?,?,?,?,?,?)",
                rid, uid, admin.id(), checkinMethod, now, "ON_TIME");
        changeCredit(uid, 5, "ON_TIME_CHECKIN", "准时签到奖励", rid);
        return reservationDetail(rid);
    }

    public List<Map<String, Object>> checkins(CurrentUser admin) {
        String extra = admin.isSuperAdmin() ? "" : sqlJoin("where", "c.admin_id=" + admin.id());
        String checkinSql = sqlJoin("""
                select c.*, sp.student_no studentNo, sp.name studentName, sr.name roomName, s.seat_no seatNo
                from checkin_record c
                join reservation r on r.id=c.reservation_id
                join student_profile sp on sp.user_id=c.user_id
                join study_room sr on sr.id=r.room_id
                join seat s on s.id=r.seat_id""", extra, "order by c.checkin_time desc");
        return jdbc.queryForList(checkinSql);
    }

    public Map<String, Object> saveAnnouncement(CurrentUser admin, Long id, Map<String, Object> req) {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            jdbc.update("insert into announcement(title,content,type,pinned,scope,room_id,publisher_id,status,published_at,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?)",
                    text(req, "title"), text(req, "content"), text(req, "type", "SYSTEM"), intText(req, "pinned", 0),
                    text(req, "scope", "GLOBAL"), nullableLong(req, "roomId"), admin.id(), text(req, "status", "PUBLISHED"), now, now, now);
            id = jdbc.queryForObject("select last_insert_id()", Long.class);
        } else {
            jdbc.update("update announcement set title=?,content=?,type=?,pinned=?,scope=?,room_id=?,status=?,updated_at=? where id=?",
                    text(req, "title"), text(req, "content"), text(req, "type", "SYSTEM"), intText(req, "pinned", 0),
                    text(req, "scope", "GLOBAL"), nullableLong(req, "roomId"), text(req, "status", "PUBLISHED"), now, id);
        }
        return one("select * from announcement where id=?", id);
    }

    public void deleteAnnouncement(Long id) {
        jdbc.update("update announcement set status='DELETED',updated_at=? where id=?", LocalDateTime.now(), id);
    }

    public List<Map<String, Object>> statisticsUsage(CurrentUser admin, String period) {
        return statisticsUsage(admin, period, null);
    }

    public List<Map<String, Object>> statisticsUsage(CurrentUser admin, String period, Long roomId) {
        return statisticsUsage(admin, period, roomId, "current");
    }

    public List<Map<String, Object>> statisticsUsage(CurrentUser admin, String period, Long roomId, String rangeMode) {
        String extra = sqlStudyRoomScopeForReservationJoin(admin, roomId);
        String dateJoin = reservationDateJoinCondition(period, rangeMode);
        String joinOn = sqlJoin("r.room_id=sr.id", "and", dateJoin);
        String usageSql = sqlJoin("""
                select sr.name roomName, sr.seat_count seatCount,
                  count(r.id) reservationCount,
                  sum(case when r.status in ('USING','COMPLETED','AUTO_CHECKOUT') then 1 else 0 end) usedCount,
                  round(if(sr.seat_count=0,0,count(r.id)/sr.seat_count*100),1) usageRate
                from study_room sr left join reservation r on""", joinOn, extra,
                "group by sr.id,sr.name,sr.seat_count", "order by sr.id");
        return jdbc.queryForList(usageSql);
    }

    public List<Map<String, Object>> statisticsPeak(CurrentUser admin, String period) {
        return statisticsPeak(admin, period, null);
    }

    public List<Map<String, Object>> statisticsPeak(CurrentUser admin, String period, Long roomId) {
        return statisticsPeak(admin, period, roomId, "current");
    }

    public List<Map<String, Object>> statisticsPeak(CurrentUser admin, String period, Long roomId, String rangeMode) {
        String dateWhere = reservationDateWhereCondition(period, "r", rangeMode);
        String whereClause = sqlWhereWithRoomScope(admin, "r", dateWhere, roomId);
        String peakSql = sqlJoin("""
                select hour(r.start_time) AS peakHour, count(*) AS cnt
                from reservation r
                join study_room sr on sr.id=r.room_id""", whereClause,
                "group by hour(r.start_time)", "order by hour(r.start_time)");
        return jdbc.queryForList(peakSql);
    }

    public List<Map<String, Object>> statisticsTrend(CurrentUser admin, String period) {
        return statisticsTrend(admin, period, null);
    }

    public List<Map<String, Object>> statisticsTrend(CurrentUser admin, String period, Long roomId) {
        return statisticsTrend(admin, period, roomId, "current");
    }

    public List<Map<String, Object>> statisticsTrend(CurrentUser admin, String period, Long roomId, String rangeMode) {
        String p = period == null ? "day" : period.toLowerCase();
        boolean past = isPastRange(rangeMode);
        if ("day".equals(p) && !past) {
            String whereClause = sqlWhereWithRoomScope(admin, "r", reservationDateWhereCondition(p, "r", rangeMode), roomId);
            String daySql = sqlJoin("""
                    select hour(r.start_time) AS peakHour, count(*) AS cnt
                    from reservation r join study_room sr on sr.id=r.room_id""", whereClause,
                    "group by hour(r.start_time)", "order by hour(r.start_time)");
            return jdbc.queryForList(daySql);
        }
        if ("year".equals(p)) {
            String whereClause = sqlWhereWithRoomScope(admin, "r", reservationDateWhereCondition(p, "r", rangeMode), roomId);
            String yearSql = sqlJoin("""
                    select month(r.reserve_date) AS monthNum, count(*) AS cnt
                    from reservation r join study_room sr on sr.id=r.room_id""", whereClause,
                    "group by month(r.reserve_date)", "order by month(r.reserve_date)");
            return jdbc.queryForList(yearSql);
        }
        String dateWhere = reservationDateWhereCondition(p, "r", rangeMode);
        String whereClause = sqlWhereWithRoomScope(admin, "r", dateWhere, roomId);
        String trendSql = sqlJoin("""
                select date_format(r.reserve_date,'%m-%d') AS timeLabel, count(*) AS cnt
                from reservation r join study_room sr on sr.id=r.room_id""", whereClause,
                "group by r.reserve_date", "order by r.reserve_date");
        return jdbc.queryForList(trendSql);
    }

    /** 今日预约：开始时间不得早于「当前时刻 - 签到宽限期」，避免一约完就被判超时 */
    private void ensureReservationStartNotTooPast(LocalDate date, LocalTime start) {
        if (!date.equals(LocalDate.now())) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CHECKIN_LATE_MINUTES);
        LocalDateTime startAt = LocalDateTime.of(date, start);
        if (startAt.isBefore(cutoff)) {
            throw new BusinessException(400, "不能预约已开始超过15分钟的时段，请选择更晚的开始时间");
        }
    }

    public Map<String, Object> statisticsReport(CurrentUser admin, String period) {
        return statisticsReport(admin, period, null);
    }

    public Map<String, Object> statisticsReport(CurrentUser admin, String period, Long roomId) {
        return statisticsReport(admin, period, roomId, "current");
    }

    public Map<String, Object> statisticsReport(CurrentUser admin, String period, Long roomId, String rangeMode) {
        String p = period == null || period.isBlank() ? "day" : period.toLowerCase();
        String mode = rangeMode == null || rangeMode.isBlank() ? "current" : rangeMode.toLowerCase();
        List<Map<String, Object>> usage = statisticsUsage(admin, p, roomId, mode);
        List<Map<String, Object>> peak = statisticsPeak(admin, p, roomId, mode);
        List<Map<String, Object>> trend = statisticsTrend(admin, p, roomId, mode);
        String dateWhere = reservationDateWhereCondition(p, "r", mode);
        String whereClause = sqlWhereWithRoomScope(admin, "r", dateWhere, roomId);
        String countFrom = "select count(*) from reservation r join study_room sr on sr.id=r.room_id";
        Integer totalReserve = jdbc.queryForObject(sqlJoin(countFrom, whereClause), Integer.class);
        Integer usingCount = jdbc.queryForObject(
                sqlJoin(countFrom, whereClause, "and r.status in ('USING','TEMP_LEAVE')"), Integer.class);
        Integer checkedIn = jdbc.queryForObject(
                sqlJoin(countFrom, whereClause, "and r.status in ('USING','COMPLETED','AUTO_CHECKOUT','TEMP_LEAVE')"),
                Integer.class);
        int total = totalReserve == null ? 0 : totalReserve;
        int checked = checkedIn == null ? 0 : checkedIn;
        int checkinRate = total == 0 ? 0 : (int) (Math.round(checked * 1000.0 / total) / 10);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("period", p);
        summary.put("rangeMode", mode);
        summary.put("periodLabel", periodLabel(p, mode));
        summary.put("rangeWindowLabel", rangeWindowLabel(p, mode));
        summary.put("roomId", roomId);
        summary.put("totalReserve", total);
        summary.put("usingCount", usingCount == null ? 0 : usingCount);
        summary.put("checkinRate", checkinRate);
        summary.put("avgCredit", averageCreditScore());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("usage", usage);
        result.put("peak", peak);
        result.put("trend", trend);
        result.put("credit", statisticsCredit());
        return result;
    }

    private int averageCreditScore() {
        Integer avg = jdbc.queryForObject("select round(avg(credit_score)) from student_profile", Integer.class);
        return avg == null ? 0 : avg;
    }

    private String periodLabel(String period, String rangeMode) {
        boolean past = isPastRange(rangeMode);
        return switch (period == null ? "day" : period.toLowerCase()) {
            case "year" -> past ? "往年年报" : "本年年报";
            case "month" -> past ? "往期月报" : "本月";
            case "week" -> past ? "往期周报" : "近7天";
            default -> past ? "往期日报" : "今日";
        };
    }

    /** 往期窗口说明：上级时间单位为窗口长度 */
    private String rangeWindowLabel(String period, String rangeMode) {
        if (!isPastRange(rangeMode)) {
            return switch (period == null ? "day" : period.toLowerCase()) {
                case "year" -> "统计范围：本年度至今";
                case "month" -> "统计范围：本月1日至今";
                case "week" -> "统计范围：近7天含今日";
                default -> "统计范围：今日";
            };
        }
        return switch (period == null ? "day" : period.toLowerCase()) {
            case "year" -> "往期窗口：上一自然年（年）";
            case "month" -> "往期窗口：近12个月（年）";
            case "week" -> "往期窗口：近30天（月）";
            default -> "往期窗口：近7天（周）";
        };
    }

    private boolean isPastRange(String rangeMode) {
        return "past".equalsIgnoreCase(rangeMode);
    }

    private String reservationDateJoinCondition(String period, String rangeMode) {
        return reservationDateWhereCondition(period, "r", rangeMode);
    }

    private String reservationDateWhereCondition(String period, String alias, String rangeMode) {
        String col = alias + ".reserve_date";
        String p = period == null ? "day" : period.toLowerCase();
        boolean past = isPastRange(rangeMode);
        return switch (p) {
            case "year" -> past
                    ? col + " between date_format(date_sub(current_date(), interval 1 year), '%Y-01-01') and date_format(date_sub(current_date(), interval 1 year), '%Y-12-31')"
                    : col + " between date_format(current_date(), '%Y-01-01') and current_date()";
            case "month" -> past
                    ? col + " between date_sub(date_format(current_date(), '%Y-%m-01'), interval 12 month) and date_sub(date_format(current_date(), '%Y-%m-01'), interval 1 day)"
                    : col + " between date_format(current_date(), '%Y-%m-01') and current_date()";
            case "week" -> past
                    ? col + " between date_sub(current_date(), interval 37 day) and date_sub(current_date(), interval 7 day)"
                    : col + " between date_sub(current_date(), interval 6 day) and current_date()";
            default -> past
                    ? col + " between date_sub(current_date(), interval 7 day) and date_sub(current_date(), interval 1 day)"
                    : col + "=current_date()";
        };
    }

    public List<Map<String, Object>> statisticsCredit() {
        return jdbc.queryForList("""
                select case
                  when credit_score>=400 then '优秀'
                  when credit_score>=300 then '良好'
                  when credit_score>=100 then '一般'
                  else '较低' end label, count(*) value
                from student_profile group by label order by value desc
                """);
    }

    public List<Map<String, Object>> adminFeedback(CurrentUser admin) {
        String extra = admin.isSuperAdmin() ? "" : sqlJoin("where", "(f.room_id is null or sr.manager_id=" + admin.id() + ")");
        String feedbackSql = sqlJoin("""
                select f.*, sp.student_no studentNo, sp.name studentName, sr.name roomName, s.seat_no seatNo
                from feedback_ticket f
                join student_profile sp on sp.user_id=f.user_id
                left join study_room sr on sr.id=f.room_id
                left join seat s on s.id=f.seat_id""", extra, "order by f.created_at desc");
        return jdbc.queryForList(feedbackSql);
    }

    public void handleFeedback(CurrentUser admin, Long id, Map<String, Object> req) {
        Map<String, Object> ticket = one("select * from feedback_ticket where id=?", id);
        if (ticket == null) {
            throw new BusinessException(404, "反馈不存在");
        }
        if ("DONE".equals(String.valueOf(ticket.get("status"))) || "CLOSED".equals(String.valueOf(ticket.get("status")))) {
            throw new BusinessException(409, "该反馈已处理完成");
        }
        String result = text(req, "handleResult", "已处理并记录");
        if (result.isBlank()) {
            throw new BusinessException(400, "请填写处理说明");
        }
        jdbc.update("update feedback_ticket set status=?,handler_id=?,handle_result=?,handled_at=? where id=?",
                text(req, "status", "DONE"), admin.id(), result, LocalDateTime.now(), id);
        Long studentId = num(ticket.get("user_id"));
        notifyUser(studentId, "反馈已处理", "你提交的问题反馈已由管理员处理：" + result, "FEEDBACK", id);
        writeOperationLog(admin, "FEEDBACK", "HANDLE", "FEEDBACK", id, result);
    }

    public Map<String, Object> updateProfile(CurrentUser user, Map<String, Object> req) {
        jdbc.update("""
                update student_profile set name=?,gender=?,college=?,major=?,grade=?,phone=?,email=?,updated_at=?
                where user_id=?
                """,
                text(req, "name", user.displayName()),
                text(req, "gender", "男"),
                text(req, "college"),
                text(req, "major"),
                text(req, "grade"),
                text(req, "phone"),
                text(req, "email"),
                LocalDateTime.now(),
                user.id());
        return studentInfo(user);
    }

    @Transactional
    public Map<String, Object> startTempLeave(CurrentUser user, Long reservationId) {
        Map<String, Object> r = reservationDetail(reservationId);
        if (!Objects.equals(num(r.get("user_id")), user.id())) {
            throw new BusinessException(403, "不能操作他人的预约");
        }
        if (!"USING".equals(String.valueOf(r.get("status")))) {
            throw new BusinessException(409, "只有使用中的预约可以暂离");
        }
        Integer active = jdbc.queryForObject(
                "select count(*) from temp_leave where reservation_id=? and leave_status='ACTIVE'", Integer.class, reservationId);
        if (active != null && active > 0) {
            throw new BusinessException(409, "当前已在暂离中");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into temp_leave(reservation_id,user_id,leave_time,leave_status,max_leave_minutes,created_at) values(?,?,?,?,?,?)",
                reservationId, user.id(), now, "ACTIVE", 30, now);
        jdbc.update("update reservation set status='TEMP_LEAVE',updated_at=? where id=?", now, reservationId);
        notifyUser(user.id(), "暂离开始", "你已暂离座位，请在 30 分钟内返回，超时将自动签退并扣分。", "TEMP_LEAVE", reservationId);
        return reservationDetail(reservationId);
    }

    @Transactional
    public Map<String, Object> endTempLeave(CurrentUser user, Long reservationId) {
        Map<String, Object> r = reservationDetail(reservationId);
        if (!Objects.equals(num(r.get("user_id")), user.id())) {
            throw new BusinessException(403, "不能操作他人的预约");
        }
        if (!"TEMP_LEAVE".equals(String.valueOf(r.get("status")))) {
            throw new BusinessException(409, "当前预约不在暂离状态");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("update temp_leave set leave_status='RETURNED',return_time=? where reservation_id=? and leave_status='ACTIVE'",
                now, reservationId);
        jdbc.update("update reservation set status='USING',updated_at=? where id=?", now, reservationId);
        notifyUser(user.id(), "暂离结束", "你已返回座位，请继续学习并在结束时签退。", "TEMP_LEAVE", reservationId);
        return reservationDetail(reservationId);
    }

    public List<Map<String, Object>> operationLogs(CurrentUser admin) {
        if (admin.isSuperAdmin()) {
            return jdbc.queryForList("select * from operation_log order by created_at desc limit 200");
        }
        return jdbc.queryForList("select * from operation_log where operator_id=? order by created_at desc limit 200", admin.id());
    }

    public List<Map<String, Object>> adminAccounts(CurrentUser admin) {
        if (!admin.isSuperAdmin()) {
            return List.of(one("select id,account,name,role,phone,status from admin_account where id=?", admin.id()));
        }
        return jdbc.queryForList("""
                select aa.id, aa.account, aa.name, aa.role, aa.phone, aa.status,
                       (select group_concat(sr.name separator '、') from study_room sr where sr.manager_id=aa.id) managedRooms
                from admin_account aa
                where aa.role in ('ADMIN','SUPER_ADMIN')
                order by aa.id
                """);
    }

    /** 超级管理员：新增图书馆管理员账号 */
    @Transactional
    public Map<String, Object> createAdminAccount(CurrentUser admin, Map<String, Object> req) {
        requireSuperAdmin(admin);
        String account = text(req, "account").trim();
        String password = text(req, "password");
        String name = text(req, "name").trim();
        if (account.isBlank() || name.isBlank() || password.length() < 6) {
            throw new BusinessException(400, "账号、姓名、密码格式不正确");
        }
        Integer exists = jdbc.queryForObject("select count(*) from admin_account where account=?", Integer.class, account);
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "管理员账号已存在");
        }
        String role = "ADMIN";
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into admin_account(account,password_hash,name,role,phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                account, passwordEncoder.encode(password), name, role, text(req, "phone"), "NORMAL", now, now);
        Long id = jdbc.queryForObject("select id from admin_account where account=?", Long.class, account);
        writeOperationLog(admin, "ADMIN", "CREATE", "ADMIN_ACCOUNT", id, account);
        return one("select id,account,name,role,phone,status from admin_account where id=?", id);
    }

    /** 超级管理员：编辑管理员资料（不含改账号） */
    public Map<String, Object> updateAdminAccount(CurrentUser admin, Long id, Map<String, Object> req) {
        requireSuperAdmin(admin);
        Map<String, Object> target = one("select * from admin_account where id=?", id);
        if (target == null) {
            throw new BusinessException(404, "管理员不存在");
        }
        String existingRole = String.valueOf(target.get("role"));
        // 接口仅维护普通图书馆管理员；不可通过此接口新增或提升为超级管理员
        String role = "SUPER_ADMIN".equals(existingRole) ? "SUPER_ADMIN" : "ADMIN";
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("update admin_account set name=?,role=?,phone=?,updated_at=? where id=?",
                text(req, "name", String.valueOf(target.get("name"))), role,
                text(req, "phone", String.valueOf(target.get("phone"))), now, id);
        String newPassword = text(req, "password");
        if (!newPassword.isBlank() && newPassword.length() >= 6) {
            jdbc.update("update admin_account set password_hash=?,updated_at=? where id=?",
                    passwordEncoder.encode(newPassword), now, id);
        }
        writeOperationLog(admin, "ADMIN", "UPDATE", "ADMIN_ACCOUNT", id, String.valueOf(target.get("account")));
        return one("select id,account,name,role,phone,status from admin_account where id=?", id);
    }

    /** 超级管理员：启用/禁用管理员 */
    public void setAdminAccountStatus(CurrentUser admin, Long id, String status) {
        requireSuperAdmin(admin);
        if (Objects.equals(id, admin.id())) {
            throw new BusinessException(400, "不能禁用当前登录账号");
        }
        if (!Set.of("NORMAL", "DISABLED").contains(status)) {
            throw new BusinessException(400, "无效状态");
        }
        jdbc.update("update admin_account set status=?,updated_at=? where id=?", status, LocalDateTime.now(), id);
        writeOperationLog(admin, "ADMIN", "NORMAL".equals(status) ? "ENABLE" : "DISABLE", "ADMIN_ACCOUNT", id, status);
    }

    private void requireSuperAdmin(CurrentUser admin) {
        if (!admin.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可执行此操作");
        }
    }

    public void scheduledProcessInvalidCheckin() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id, user_id, reserve_date, start_time, end_time, sign_in_time
                from reservation where status in ('USING','TEMP_LEAVE')
                """);
        for (Map<String, Object> row : rows) {
            LocalDateTime signIn = toLocalDateTime(row.get("sign_in_time"));
            if (signIn == null || !isWithinCheckinWindow(row, signIn)) {
                revertInvalidCheckin(num(row.get("id")), num(row.get("user_id")));
            }
        }
    }

    public void scheduledProcessNoShow() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id,user_id,reserve_date,start_time from reservation where status='PENDING'
                """);
        for (Map<String, Object> row : rows) {
            LocalDateTime startAt = LocalDateTime.of(
                    ((Date) row.get("reserve_date")).toLocalDate(),
                    ((Time) row.get("start_time")).toLocalTime());
            if (!startAt.isBefore(deadline)) {
                continue;
            }
            Long id = num(row.get("id"));
            Long userId = num(row.get("user_id"));
            jdbc.update("update reservation set status='VIOLATED',cancel_reason='超时未签到',updated_at=? where id=? and status='PENDING'",
                    LocalDateTime.now(), id);
            releaseReservationSlots(id);
            changeCredit(userId, -50, "NO_SHOW", "预约超时未签到", id);
            notifyUser(userId, "预约违约", "你有一条预约因超时未签到被判定违约，已扣除 50 信用分。", "VIOLATION", id);
        }
    }

    public void scheduledProcessAutoCheckout() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select id,user_id,sign_in_time,reserve_date,end_time from reservation
                where status in ('USING','TEMP_LEAVE')
                  and timestamp(reserve_date,end_time) < now()
                """);
        for (Map<String, Object> row : rows) {
            autoCheckoutReservation(num(row.get("id")), num(row.get("user_id")), toLocalDateTime(row.get("sign_in_time")),
                    LocalDateTime.of(((Date) row.get("reserve_date")).toLocalDate(), ((Time) row.get("end_time")).toLocalTime()),
                    "AUTO_CHECKOUT", "预约时段结束，系统已自动签退");
        }
    }

    public void scheduledProcessBlacklistRelease() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select user_id,id from blacklist_record where status='ACTIVE' and end_time <= ?
                """, LocalDateTime.now());
        for (Map<String, Object> row : rows) {
            Long userId = num(row.get("user_id"));
            jdbc.update("update blacklist_record set status='RELEASED',released_at=? where id=?", LocalDateTime.now(), row.get("id"));
            jdbc.update("update user_account set status='NORMAL',updated_at=? where id=?", LocalDateTime.now(), userId);
            jdbc.update("update student_profile set credit_score=10,updated_at=? where user_id=?", LocalDateTime.now(), userId);
            jdbc.update("insert into credit_log(user_id,before_score,change_value,after_score,change_type,reason,created_at) values(?,?,?,?,?,?,?)",
                    userId, 0, 10, 10, "BLACKLIST_RELEASE", "黑名单解除，积分恢复为 10", LocalDateTime.now());
            notifyUser(userId, "黑名单解除", "你的黑名单已到期，账号已恢复，信用积分重置为 10 分。", "BLACKLIST", num(row.get("id")));
        }
    }

    public void scheduledProcessTempLeaveTimeout() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select tl.reservation_id,tl.user_id,r.sign_in_time,r.reserve_date,r.end_time
                from temp_leave tl
                join reservation r on r.id=tl.reservation_id
                where tl.leave_status='ACTIVE'
                  and tl.leave_time < date_sub(now(), interval tl.max_leave_minutes minute)
                  and r.status='TEMP_LEAVE'
                """);
        for (Map<String, Object> row : rows) {
            Long reservationId = num(row.get("reservation_id"));
            Long userId = num(row.get("user_id"));
            jdbc.update("update temp_leave set leave_status='TIMEOUT',return_time=? where reservation_id=? and leave_status='ACTIVE'",
                    LocalDateTime.now(), reservationId);
            LocalDateTime endAt = LocalDateTime.of(((Date) row.get("reserve_date")).toLocalDate(), ((Time) row.get("end_time")).toLocalTime());
            autoCheckoutReservation(reservationId, userId, toLocalDateTime(row.get("sign_in_time")), endAt.isBefore(LocalDateTime.now()) ? endAt : LocalDateTime.now(),
                    "AUTO_CHECKOUT", "暂离超时，系统已自动签退");
            changeCredit(userId, -30, "TEMP_LEAVE_TIMEOUT", "暂离超时", reservationId);
            notifyUser(userId, "暂离超时", "暂离超过 30 分钟，系统已自动签退并扣除 30 信用分。", "TEMP_LEAVE", reservationId);
        }
    }

    public String exportCsv(CurrentUser admin, String period) {
        return exportCsv(admin, period, null);
    }

    public String exportCsv(CurrentUser admin, String period, Long roomId) {
        return exportCsv(admin, period, roomId, "current");
    }

    public String exportCsv(CurrentUser admin, String period, Long roomId, String rangeMode) {
        String label = periodLabel(period == null ? "day" : period.toLowerCase(), rangeMode);
        StringBuilder csv = new StringBuilder("统计周期,").append(label).append("\n");
        csv.append(rangeWindowLabel(period == null ? "day" : period.toLowerCase(), rangeMode)).append("\n");
        if (roomId != null) {
            Map<String, Object> roomInfo = room(roomId);
            csv.append("自习室,").append(roomInfo.get("name")).append("\n");
        }
        csv.append("自习室,总座位,预约数,实际使用数,使用率\n");
        for (Map<String, Object> row : statisticsUsage(admin, period, roomId, rangeMode)) {
            csv.append(row.get("roomName")).append(',')
                    .append(row.get("seatCount")).append(',')
                    .append(row.get("reservationCount")).append(',')
                    .append(row.get("usedCount")).append(',')
                    .append(row.get("usageRate")).append("%\n");
        }
        return csv.toString();
    }

    private LocalDateTime reservationStartAt(Map<String, Object> r) {
        return LocalDateTime.of(
                ((Date) r.get("reserve_date")).toLocalDate(),
                ((Time) r.get("start_time")).toLocalTime());
    }

    private LocalDateTime reservationEndAt(Map<String, Object> r) {
        return LocalDateTime.of(
                ((Date) r.get("reserve_date")).toLocalDate(),
                ((Time) r.get("end_time")).toLocalTime());
    }

    private LocalDateTime checkinWindowStart(Map<String, Object> r) {
        return reservationStartAt(r).minusMinutes(CHECKIN_EARLY_MINUTES);
    }

    private LocalDateTime checkinWindowEnd(Map<String, Object> r) {
        return reservationStartAt(r).plusMinutes(CHECKIN_LATE_MINUTES);
    }

    private boolean isWithinCheckinWindow(Map<String, Object> r, LocalDateTime at) {
        return !at.isBefore(checkinWindowStart(r)) && !at.isAfter(checkinWindowEnd(r));
    }

    private void ensureWithinCheckinWindow(Map<String, Object> r) {
        LocalDateTime now = LocalDateTime.now();
        if (isWithinCheckinWindow(r, now)) {
            return;
        }
        LocalDateTime start = checkinWindowStart(r);
        LocalDateTime end = checkinWindowEnd(r);
        throw new BusinessException(400, String.format(
                "当前不在签到时间内，请在 %s %s 至 %s 之间签到",
                ((Date) r.get("reserve_date")).toLocalDate(),
                start.format(CHECKIN_WINDOW_FMT),
                end.format(CHECKIN_WINDOW_FMT)));
    }

    private void revertInvalidCheckin(Long reservationId, Long userId) {
        jdbc.update("delete from checkin_record where reservation_id=?", reservationId);
        jdbc.update("update temp_leave set leave_status='CANCELLED',return_time=? where reservation_id=? and leave_status='ACTIVE'",
                LocalDateTime.now(), reservationId);
        int updated = jdbc.update(
                "update reservation set status='PENDING',sign_in_time=null,updated_at=? where id=? and status in ('USING','TEMP_LEAVE')",
                LocalDateTime.now(), reservationId);
        if (updated > 0) {
            changeCredit(userId, -5, "INVALID_CHECKIN_REVERT", "无效签到已撤销", reservationId);
            notifyUser(userId, "签到无效", "你的签到不在有效时间内，已恢复为待签到状态。", "CHECKIN", reservationId);
        }
    }

    private void autoCheckoutReservation(Long reservationId, Long userId, LocalDateTime signIn, LocalDateTime checkoutAt,
                                         String status, String reason) {
        int minutes = signIn == null ? 0 : (int) Math.max(0, Duration.between(signIn, checkoutAt).toMinutes());
        jdbc.update("update reservation set status=?,sign_out_time=?,actual_minutes=?,updated_at=? where id=? and status in ('USING','TEMP_LEAVE')",
                status, checkoutAt, minutes, LocalDateTime.now(), reservationId);
        jdbc.update("update checkin_record set checkout_time=? where reservation_id=?", checkoutAt, reservationId);
        releaseReservationSlots(reservationId);
        notifyUser(userId, "自动签退", reason, "CHECKOUT", reservationId);
    }

    /** 释放预约占用的座位时间片（直接删除，避免 RELEASED 与唯一索引冲突） */
    private void releaseReservationSlots(Long reservationId) {
        if (reservationId == null) {
            return;
        }
        jdbc.update("delete from reservation_slot where reservation_id=?", reservationId);
    }

    private void notifyUser(Long userId, String title, String content, String type, Long relatedId) {
        jdbc.update("insert into notification_message(user_id,title,content,type,related_id,created_at) values(?,?,?,?,?,?)",
                userId, title, content, type, relatedId, LocalDateTime.now());
    }

    private void writeOperationLog(CurrentUser admin, String module, String action, String targetType, Long targetId, String detail) {
        if (admin == null) {
            return;
        }
        jdbc.update("insert into operation_log(operator_id,operator_name,module,action,target_type,target_id,detail,created_at) values(?,?,?,?,?,?,?,?)",
                admin.id(), admin.displayName(), module, action, targetType, targetId, detail, LocalDateTime.now());
    }

    private void decorateRoom(Map<String, Object> room) {
        Long id = num(room.get("id"));
        Integer occupied = jdbc.queryForObject("""
                select count(distinct rs.seat_id)
                from reservation_slot rs join seat s on s.id=rs.seat_id
                where s.room_id=? and rs.status='ACTIVE' and rs.slot_start>=? and rs.slot_start<?
                """, Integer.class, id, LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay());
        int normal = room.containsKey("normalSeatCount") ? intValue(room.get("normalSeatCount")) :
                Optional.ofNullable(jdbc.queryForObject("select count(*) from seat where room_id=? and is_seat=1 and status='NORMAL'", Integer.class, id)).orElse(0);
        room.put("availableSeats", Math.max(0, normal - (occupied == null ? 0 : occupied)));
        room.put("openTimeText", room.get("open_time") + "-" + room.get("close_time"));
    }

    private void recreateSeats(Long roomId, int rows, int cols, String prefix) {
        LocalDateTime now = LocalDateTime.now();
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                jdbc.update("insert into seat(room_id,seat_no,row_no,col_no,is_seat,cell_category,seat_type,has_power,near_window,quiet_zone,hot_seat,status,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        roomId, prefix + "-" + String.format("%02d", (r - 1) * cols + c), r, c, 1, "SEAT", "普通座位",
                        c % 2 == 0 ? 1 : 0, c == 1 || c == cols ? 1 : 0, r <= 2 ? 1 : 0, 0, "NORMAL", now, now);
            }
        }
    }

    /**
     * 自习室行列变更时同步座位表：按 (row_no,col_no) 补齐/删除，与 study_room.row_count/col_count 严格一致。
     */
    private void syncRoomSeatGrid(Long roomId, int rows, int cols, String roomCode) {
        String prefix = String.valueOf(roomCode).replaceAll("[^A-Za-z0-9]", "");
        if (prefix.isBlank()) {
            prefix = "S";
        }
        List<Map<String, Object>> existing = jdbc.queryForList("select * from seat where room_id=? order by id", roomId);
        Map<String, Map<String, Object>> cellMap = new LinkedHashMap<>();
        for (Map<String, Object> seat : existing) {
            String key = intValue(seat.get("row_no")) + ":" + intValue(seat.get("col_no"));
            if (!cellMap.containsKey(key)) {
                cellMap.put(key, seat);
                continue;
            }
            Long dupId = num(seat.get("id"));
            Integer active = jdbc.queryForObject("""
                    select count(*) from reservation
                    where seat_id=? and status in ('PENDING','USING','TEMP_LEAVE')
                    """, Integer.class, dupId);
            if (active != null && active > 0) {
                throw new BusinessException(409, "存在重复格子且含进行中预约，请先合并或删除重复座位");
            }
            jdbc.update("delete from seat where id=?", dupId);
        }
        LocalDateTime now = LocalDateTime.now();
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                String key = r + ":" + c;
                String seatNo = prefix + "-" + String.format("%02d", (r - 1) * cols + c);
                if (cellMap.containsKey(key)) {
                    jdbc.update("update seat set seat_no=?,row_no=?,col_no=?,updated_at=? where id=?",
                            seatNo, r, c, now, num(cellMap.get(key).get("id")));
                } else {
                    jdbc.update("""
                            insert into seat(room_id,seat_no,row_no,col_no,is_seat,cell_category,seat_type,has_power,near_window,quiet_zone,hot_seat,status,created_at,updated_at)
                            values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                            """, roomId, seatNo, r, c, 1, "SEAT", "普通座位",
                            c % 2 == 0 ? 1 : 0, c == 1 || c == cols ? 1 : 0, r <= 2 ? 1 : 0, 0, "NORMAL", now, now);
                }
            }
        }
        for (Map<String, Object> seat : existing) {
            int r = intValue(seat.get("row_no"));
            int c = intValue(seat.get("col_no"));
            if (r <= rows && c <= cols) {
                continue;
            }
            Long seatId = num(seat.get("id"));
            Integer active = jdbc.queryForObject("""
                    select count(*) from reservation
                    where seat_id=? and status in ('PENDING','USING','TEMP_LEAVE')
                    """, Integer.class, seatId);
            if (active != null && active > 0) {
                throw new BusinessException(409, "缩小行列数前，请先处理超出范围的进行中预约座位");
            }
            jdbc.update("delete from seat where id=?", seatId);
        }
        Integer seatCount = jdbc.queryForObject("select count(*) from seat where room_id=? and is_seat=1", Integer.class, roomId);
        jdbc.update("update study_room set seat_count=?,cell_count=?,updated_at=? where id=?",
                seatCount == null ? 0 : seatCount, rows * cols, now, roomId);
    }

    private void changeCredit(Long userId, int delta, String type, String reason, Long reservationId) {
        Map<String, Object> profile = one("select credit_score from student_profile where user_id=?", userId);
        int before = intValue(profile.get("credit_score"));
        int after = Math.max(0, Math.min(CREDIT_SCORE_MAX, before + delta));
        jdbc.update("update student_profile set credit_score=?,updated_at=? where user_id=?", after, LocalDateTime.now(), userId);
        jdbc.update("insert into credit_log(user_id,before_score,change_value,after_score,change_type,reason,reservation_id,created_at) values(?,?,?,?,?,?,?,?)",
                userId, before, delta, after, type, reason, reservationId, LocalDateTime.now());
        if (after <= 0) {
            jdbc.update("insert into blacklist_record(user_id,start_time,end_time,reason,status) values(?,?,?,?,?)",
                    userId, LocalDateTime.now(), LocalDateTime.now().plusDays(7), "信用积分小于等于0", "ACTIVE");
            jdbc.update("update user_account set status='BLACKLIST',updated_at=? where id=?", LocalDateTime.now(), userId);
        }
    }

    private static String sqlJoin(String... parts) {
        return SqlFragments.join(parts);
    }

    /** WHERE 子句 + 管理员自习室范围，可选指定 roomId */
    private String sqlWhereWithRoomScope(CurrentUser admin, String reservationAlias, String datePredicate, Long roomId) {
        String clause = sqlJoin("where", datePredicate);
        if (roomId != null) {
            assertRoomStatsAccess(admin, roomId);
            clause = sqlJoin(clause, "and", reservationAlias + ".room_id=" + roomId);
        } else if (!admin.isSuperAdmin()) {
            clause = sqlJoin(clause, "and", "sr.manager_id=" + admin.id());
        }
        return clause;
    }

    /** 用于 study_room LEFT JOIN reservation 后的 WHERE（管理员范围 + 可选 roomId） */
    private String sqlStudyRoomScopeForReservationJoin(CurrentUser admin, Long roomId) {
        if (roomId != null) {
            assertRoomStatsAccess(admin, roomId);
            return " where sr.id=" + roomId;
        }
        return admin.isSuperAdmin() ? "" : " where sr.manager_id=" + admin.id();
    }

    private void assertRoomStatsAccess(CurrentUser admin, Long roomId) {
        Map<String, Object> room = room(roomId);
        if (room == null) {
            throw new BusinessException(404, "自习室不存在");
        }
        if (!admin.isSuperAdmin() && !Objects.equals(num(room.get("manager_id")), admin.id())) {
            throw new BusinessException(403, "无权限查看该自习室统计");
        }
    }


    /** 自习室表范围：普通管理员仅看自己管理的自习室 */
    private String sqlStudyRoomScope(CurrentUser admin) {
        return admin.isSuperAdmin() ? "" : " where manager_id=" + admin.id() + " ";
    }

    /** 预约联表范围：拼接在 WHERE 之后，首尾带空格，避免 SQL 粘连 */
    private String sqlReservationRoomScope(CurrentUser admin) {
        return admin.isSuperAdmin() ? "" : " and sr.manager_id=" + admin.id() + " ";
    }

    private List<LocalDateTime> slots(LocalDate date, LocalTime start, LocalTime end) {
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(date, start);
        LocalDateTime endAt = LocalDateTime.of(date, end);
        while (cursor.isBefore(endAt)) {
            result.add(cursor);
            cursor = cursor.plusMinutes(10);
        }
        return result;
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException(400, "开始时间必须早于结束时间");
        }
        if (Duration.between(start, end).toMinutes() % 10 != 0) {
            throw new BusinessException(400, "预约时间必须按 10 分钟对齐");
        }
    }

    /** 预约时段须在自习室 open_time ~ close_time 内（与前端下拉选项一致） */
    private void validateTimeWithinRoom(LocalTime start, LocalTime end, Map<String, Object> room) {
        LocalTime open = parseTime(String.valueOf(room.get("open_time")));
        LocalTime close = parseTime(String.valueOf(room.get("close_time")));
        if (start.isBefore(open) || end.isAfter(close)) {
            throw new BusinessException(400, "预约时段须在自习室开放时间内（"
                    + open.format(CHECKIN_WINDOW_FMT) + "-" + close.format(CHECKIN_WINDOW_FMT) + "）");
        }
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String text(Map<String, Object> req, String key) {
        return text(req, key, "");
    }

    private String text(Map<String, Object> req, String key, String fallback) {
        Object value = req.get(key);
        if (value == null && req.containsKey(fallback)) {
            value = req.get(fallback);
        }
        String text = value == null ? fallback : String.valueOf(value).trim();
        return text;
    }

    private Long longText(Map<String, Object> req, String key) {
        Long value = nullableLong(req, key);
        if (value == null) {
            throw new BusinessException(400, key + "不能为空");
        }
        return value;
    }

    private Long nullableLong(Map<String, Object> req, String key) {
        Object value = req.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int intText(Map<String, Object> req, String key, int fallback) {
        Object value = req.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value);
    }

    private LocalTime parseTime(String value) {
        if (value.length() == 5) {
            value += ":00";
        }
        return LocalTime.parse(value);
    }

    private Long num(Object value) {
        return ((Number) value).longValue();
    }

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).intValue();
    }

    private boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && intValue(value) != 0;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }
}
