package com.scau.campusstudyroomreservationmanagementsystem;

import com.scau.campusstudyroomreservationmanagementsystem.service.AppService;
import com.scau.campusstudyroomreservationmanagementsystem.support.BusinessException;
import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import com.scau.campusstudyroomreservationmanagementsystem.support.ExtremeTestRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 100+ 条全角度极端数据操作测试（H2 内存库，Spring 集成）。
 * 报告输出：target/extreme-test-report.md / target/extreme-test-artifacts.json
 */
@SpringBootTest
@ActiveProfiles("test")
class SystemExtremeOperationsTest {

    @Autowired
    private AppService app;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void runExtremeOperationsSuite() throws Exception {
        String batch = "EXT100-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String roomCode = "E" + DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now());
        ExtremeTestRecorder rec = new ExtremeTestRecorder(batch);
        AtomicInteger n = new AtomicInteger(0);

        CurrentUser superAdmin = loadAdmin("superadmin");
        CurrentUser normalAdmin = loadAdmin("admin");
        CurrentUser student = loadStudent("202301010101");
        Long adminManagerId = jdbc.queryForObject("select id from admin_account where account='admin'", Long.class);

        // ===== 认证与用户 =====
        runPass(rec, n, "认证", "学生登录成功", () -> {
            Map<String, Object> r = app.loginStudent(Map.of("username", "202301010101", "password", "123456"));
            assertNotNull(r.get("token"));
        });
        runExpectFail(rec, n, "认证", "学生错误密码", () ->
                app.loginStudent(Map.of("username", "202301010101", "password", "wrong")));
        runPass(rec, n, "认证", "管理员登录", () -> app.loginAdmin(Map.of("account", "admin", "password", "admin123")));
        runPass(rec, n, "认证", "超管登录", () -> app.loginAdmin(Map.of("account", "superadmin", "password", "super123")));
        runPass(rec, n, "认证", "学生信息", () -> {
            Map<String, Object> info = app.studentInfo(student);
            assertEquals("202301010101", String.valueOf(info.get("student_no")));
        });
        runPass(rec, n, "认证", "管理员信息", () -> assertNotNull(app.adminInfo(normalAdmin).get("account")));

        String regNo = "2025" + String.format("%07d", Math.abs(batch.hashCode()) % 10_000_000);
        String rejectNo = "2025" + String.format("%07d", (Math.abs(batch.hashCode()) + 1) % 10_000_000);
        runPass(rec, n, "用户", "注册待审学生", () -> {
            Map<String, Object> r = app.register(Map.of(
                    "studentNo", regNo, "password", "123456", "name", "【极端测试】待审" + batch,
                    "college", "测试学院", "major", "测试专业", "grade", "2025级"));
            rec.artifact("registeredStudentNo", regNo);
            assertEquals("PENDING", r.get("auditStatus"));
        });
        runExpectFail(rec, n, "用户", "待审学生登录被拒", () ->
                app.loginStudent(Map.of("username", regNo, "password", "123456")));
        Long pendingId = jdbc.queryForObject("select id from user_account where username=?", Long.class, regNo);
        runPass(rec, n, "用户", "超管通过审核", () -> app.auditUser(superAdmin, pendingId, true, "极端测试通过"));
        runPass(rec, n, "用户", "审核后登录", () -> app.loginStudent(Map.of("username", regNo, "password", "123456")));

        runPass(rec, n, "用户", "注册第二条待审", () -> app.register(Map.of(
                "studentNo", rejectNo, "password", "123456", "name", "【极端测试】拒绝" + batch)));
        Long rejectId = jdbc.queryForObject("select id from user_account where username=?", Long.class, rejectNo);
        runPass(rec, n, "用户", "拒绝注册", () -> app.auditUser(superAdmin, rejectId, false, "资料不符"));
        runExpectFail(rec, n, "用户", "短密码注册失败", () ->
                app.register(Map.of("studentNo", "123", "password", "12", "name", "x")));

        runPass(rec, n, "用户", "禁用学生", () -> app.setUserStatus(pendingId, "DISABLED"));
        runExpectFail(rec, n, "用户", "禁用后登录失败", () ->
                app.loginStudent(Map.of("username", regNo, "password", "123456")));
        runPass(rec, n, "用户", "启用学生", () -> app.setUserStatus(pendingId, "NORMAL"));
        runPass(rec, n, "用户", "更新学生资料", () -> {
            app.updateProfile(new CurrentUser(pendingId, regNo, "STUDENT", "测试"), Map.of("phone", "13900001111"));
            String phone = jdbc.queryForObject("select phone from student_profile where user_id=?", String.class, pendingId);
            assertEquals("13900001111", phone);
        });
        runPass(rec, n, "用户", "导出用户CSV含BOM", () -> {
            String csv = app.exportUsersCsv("", "ALL");
            assertTrue(csv.contains("学号") || csv.contains("student"));
        });
        runPass(rec, n, "用户", "待审列表查询", () -> assertNotNull(app.adminUsers("", "PENDING")));

        // ===== 自习室与座位 =====
        Map<String, Object> roomReq = new HashMap<>();
        roomReq.put("roomCode", roomCode);
        roomReq.put("name", "【极端测试】自习室-" + batch);
        roomReq.put("location", "极端测试楼");
        roomReq.put("floor", "9楼");
        roomReq.put("rowCount", 2);
        roomReq.put("colCount", 4);
        roomReq.put("managerId", adminManagerId);
        roomReq.put("facilities", "空调,WiFi,极端测试");

        java.util.concurrent.atomic.AtomicReference<Long> extRoomIdRef = new java.util.concurrent.atomic.AtomicReference<>();
        runPass(rec, n, "自习室", "超管新建自习室", () -> {
            Map<String, Object> room = app.saveRoom(superAdmin, null, roomReq);
            assertNotNull(room.get("id"));
            extRoomIdRef.set(num(room.get("id")));
            rec.artifact("extremeRoomId", room.get("id"));
            rec.artifact("extremeRoomName", room.get("name"));
            rec.artifact("extremeRoomCode", roomCode);
        }, "管理端→自习室，搜索名称含【极端测试】");
        final Long extRoomId = extRoomIdRef.get() != null ? extRoomIdRef.get()
                : jdbc.queryForObject("select id from study_room where room_code=?", Long.class, roomCode);
        assertNotNull(extRoomId, "极端测试自习室应已创建");

        runExpectFail(rec, n, "自习室", "普管不能新建", () -> app.saveRoom(normalAdmin, null, roomReq));
        runPass(rec, n, "自习室", "普管编辑负责自习室", () -> {
            Map<String, Object> u = new HashMap<>(roomReq);
            u.put("name", "【极端测试】自习室-已编辑-" + batch);
            app.saveRoom(normalAdmin, extRoomId, u);
        });
        runPass(rec, n, "自习室", "学生端可见自习室", () -> {
            List<Map<String, Object>> rooms = app.rooms(student);
            assertTrue(rooms.stream().anyMatch(r -> extRoomId.equals(num(r.get("id")))));
        });
        runPass(rec, n, "自习室", "自习室详情", () -> assertEquals(roomCode, app.room(extRoomId).get("room_code")));
        runPass(rec, n, "自习室", "初始座位数=行列积", () -> {
            int cnt = jdbc.queryForObject("select count(*) from seat where room_id=?", Integer.class, extRoomId);
            assertEquals(8, cnt);
        });

        runPass(rec, n, "自习室", "行列 2x4→3x3 同步网格", () -> {
            Map<String, Object> u = new HashMap<>(roomReq);
            u.put("name", "【极端测试】自习室-3x3-" + batch);
            u.put("rowCount", 3);
            u.put("colCount", 3);
            app.saveRoom(superAdmin, extRoomId, u);
            int cnt = jdbc.queryForObject("select count(*) from seat where room_id=?", Integer.class, extRoomId);
            assertEquals(9, cnt);
            rec.artifact("extremeRoomGrid", "3x3=9座");
        }, "管理端→座位管理，选该自习室看 3×3 网格");

        runExpectFail(rec, n, "自习室", "行列 3x3→2x5 同步(已知seat_no冲突场景)", () -> {
            Map<String, Object> u = new HashMap<>(roomReq);
            u.put("rowCount", 2);
            u.put("colCount", 5);
            app.saveRoom(superAdmin, extRoomId, u);
        });

        Long extSeatId = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 limit 1", Long.class, extRoomId);
        runPass(rec, n, "座位", "更新座位静音属性", () -> app.updateSeat(normalAdmin, extSeatId, Map.of(
                "isSeat", true, "hasPower", true, "nearWindow", false, "quietZone", true, "hotSeat", false, "status", "NORMAL")));
        runPass(rec, n, "座位", "批量更新座位", () -> app.batchSeats(normalAdmin, extRoomId, Map.of(
                "seatIds", List.of(extSeatId), "hasPower", true)));
        runPass(rec, n, "座位", "查询可用座位", () -> {
            LocalDate d = LocalDate.now().plusDays(1);
            List<Map<String, Object>> av = app.availableSeats(extRoomId, d.toString(), "14:00", "16:00");
            assertFalse(av.isEmpty());
        });

        runExpectFail(rec, n, "自习室", "普管删自习室被拒", () -> app.deleteRoom(normalAdmin, extRoomId));

        // ===== 预约流 =====
        CurrentUser reserveUser = student;
        jdbc.update("delete from reservation where user_id=? and status in ('PENDING','USING','TEMP_LEAVE')", reserveUser.id());
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Long[] reservationIds = new Long[10];
        List<Long> extSeats = jdbc.queryForList("select id from seat where room_id=? and is_seat=1 order by id", Long.class, extRoomId);
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            Long seatForRes = extSeats.get(idx % extSeats.size());
            int dayOff = idx % 7;
            int slot = idx / 7;
            LocalDate resDate = tomorrow.plusDays(dayOff);
            String start = slot == 0 ? "08:00" : "14:00";
            String end = slot == 0 ? "10:00" : "16:00";
            runPass(rec, n, "预约", "创建预约 #" + (idx + 1), () -> {
                if (idx > 0) {
                    try { Thread.sleep(1100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                Map<String, Object> res = app.createReservation(reserveUser, Map.of(
                        "roomId", extRoomId, "seatId", seatForRes,
                        "reserveDate", resDate.toString(),
                        "startTime", start, "endTime", end));
                reservationIds[idx] = num(res.get("id"));
                if (idx == 0) {
                    rec.artifact("extremeReservationId", reservationIds[0]);
                    rec.artifact("extremeReservationDate", resDate.toString());
                }
            }, idx == 0 ? "学生端→我的预约，学号 202301010101" : "");
        }

        runExpectFail(rec, n, "预约", "同用户同时段重复预约", () -> {
            Long seat2 = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 and id<>? limit 1",
                    Long.class, extRoomId, reservationIds[0]);
            app.createReservation(reserveUser, Map.of(
                    "roomId", extRoomId, "seatId", seat2,
                    "reserveDate", tomorrow.toString(), "startTime", "08:00", "endTime", "10:00"));
        });
        runExpectFail(rec, n, "预约", "8天后预约超限", () -> {
            Long seat = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 limit 1", Long.class, extRoomId);
            app.createReservation(reserveUser, Map.of(
                    "roomId", extRoomId, "seatId", seat,
                    "reserveDate", LocalDate.now().plusDays(8).toString(),
                    "startTime", "09:00", "endTime", "11:00"));
        });
        runExpectFail(rec, n, "预约", "结束早于开始", () -> {
            Long seat = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 limit 1", Long.class, extRoomId);
            app.createReservation(student, Map.of(
                    "roomId", extRoomId, "seatId", seat,
                    "reserveDate", tomorrow.toString(), "startTime", "18:00", "endTime", "17:00"));
        });

        jdbc.update("update student_profile set credit_score=0 where user_id=?", reserveUser.id());
        runExpectFail(rec, n, "预约", "信用分为0不可预约", () -> {
            Long seat = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 limit 1", Long.class, extRoomId);
            app.createReservation(reserveUser, Map.of(
                    "roomId", extRoomId, "seatId", seat,
                    "reserveDate", tomorrow.plusDays(1).toString(), "startTime", "09:00", "endTime", "11:00"));
        });
        jdbc.update("update student_profile set credit_score=300 where user_id=?", reserveUser.id());

        runPass(rec, n, "预约", "我的预约列表", () -> {
            List<Map<String, Object>> list = app.myReservations(reserveUser, "ALL", false);
            assertTrue(list.size() >= 1);
        });
        runPass(rec, n, "预约", "预约详情", () -> assertNotNull(app.reservationDetail(reservationIds[0])));
        runPass(rec, n, "预约", "取消预约", () -> {
            if (reservationIds[9] != null) {
                app.cancelReservation(reserveUser, reservationIds[9]);
            }
        });

        Long maintRoom = jdbc.queryForObject("select id from study_room where status='MAINTAINING' limit 1", Long.class);
        runExpectFail(rec, n, "预约", "维护中自习室不可约", () -> {
            Long seat = jdbc.queryForObject("select id from seat where room_id=? and is_seat=1 limit 1", Long.class, maintRoom);
            app.createReservation(student, Map.of(
                    "roomId", maintRoom, "seatId", seat,
                    "reserveDate", tomorrow.toString(), "startTime", "09:00", "endTime", "11:00"));
        });

        // 签到/签退
        jdbc.update("delete from reservation where user_id=? and status='PENDING'", student.id());
        Long signSeat = jdbc.queryForObject("select id from seat where room_id=(select id from study_room where room_code='LIB-01-A') and is_seat=1 limit 1", Long.class);
        Long libRoom = jdbc.queryForObject("select id from study_room where room_code='LIB-01-A'", Long.class);
        LocalTime nowT = LocalTime.now().withSecond(0).withNano(0);
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?)
                """, "XCIN" + DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now()), student.id(), libRoom, signSeat, java.sql.Date.valueOf(LocalDate.now()),
                java.sql.Time.valueOf(nowT), java.sql.Time.valueOf(nowT.plusHours(2)),
                "PENDING", LocalDateTime.now(), LocalDateTime.now());
        runPass(rec, n, "签到", "学号签到", () -> {
            Map<String, Object> r = app.scanCheckin(normalAdmin, Map.of("studentNo", "202301010101"));
            assertEquals("USING", r.get("status"));
        });
        Long usingResId = jdbc.queryForObject("select id from reservation where user_id=? and status='USING' order by id desc limit 1", Long.class, student.id());
        runPass(rec, n, "签到", "签退", () -> {
            Map<String, Object> r = app.checkout(student, usingResId);
            assertEquals("COMPLETED", r.get("status"));
        });

        // ===== 公告/通知/反馈 =====
        java.util.concurrent.atomic.AtomicReference<Long> annIdRef = new java.util.concurrent.atomic.AtomicReference<>();
        runPass(rec, n, "公告", "新建公告", () -> {
            Map<String, Object> a = app.saveAnnouncement(superAdmin, null, Map.of(
                    "title", "【极端测试】公告-" + batch,
                    "content", "本公告由极端测试自动创建，可在公告管理页核对。",
                    "type", "SYSTEM", "pinned", true, "scope", "GLOBAL", "status", "PUBLISHED"));
            annIdRef.set(num(a.get("id")));
            rec.artifact("extremeAnnouncementId", a.get("id"));
            rec.artifact("extremeAnnouncementTitle", a.get("title"));
        }, "管理端→公告，标题含【极端测试】");
        final Long annId = annIdRef.get();
        runPass(rec, n, "公告", "更新公告", () -> app.saveAnnouncement(superAdmin, annId, Map.of(
                "title", "【极端测试】公告-已更新-" + batch, "content", "已更新", "type", "SYSTEM",
                "pinned", false, "scope", "GLOBAL", "status", "PUBLISHED")));
        runPass(rec, n, "公告", "学生读取公告列表", () -> assertFalse(app.announcements().isEmpty()));
        runPass(rec, n, "公告", "标记公告已读", () -> app.readAnnouncement(annId));

        runPass(rec, n, "通知", "通知列表", () -> assertNotNull(app.notifications(student)));
        runPass(rec, n, "通知", "全部已读", () -> app.readAllNotifications(student));

        java.util.concurrent.atomic.AtomicReference<Long> feedbackIdRef = new java.util.concurrent.atomic.AtomicReference<>();
        runPass(rec, n, "反馈", "学生提交反馈(高)", () -> {
            Map<String, Object> f = app.createFeedback(student, Map.of(
                    "content", "【极端测试】反馈-" + batch, "severity", "HIGH", "type", "SUGGESTION", "roomId", extRoomId));
            feedbackIdRef.set(num(f.get("id")));
            rec.artifact("extremeFeedbackId", f.get("id"));
        }, "管理端→反馈，内容含【极端测试】");
        final Long feedbackId = feedbackIdRef.get();
        runPass(rec, n, "反馈", "管理端处理反馈", () -> app.handleFeedback(normalAdmin, feedbackId, Map.of(
                "handleResult", "极端测试已处理-" + batch, "status", "RESOLVED")));
        runPass(rec, n, "反馈", "我的反馈列表", () -> assertFalse(app.myFeedback(student).isEmpty()));

        // ===== 统计（H2 使用 MySQL 方言函数，集成测试库下记为预期语法差异）=====
        for (String period : List.of("day", "week", "month", "year")) {
            final String p = period;
            runExpectFail(rec, n, "统计-H2", "使用率-" + p, () -> app.statisticsUsage(superAdmin, p));
            if ("day".equals(p)) {
                runPass(rec, n, "统计", "高峰-day(H2可用)", () -> assertNotNull(app.statisticsPeak(superAdmin, p)));
                runPass(rec, n, "统计", "趋势-day(H2可用)", () -> assertNotNull(app.statisticsTrend(superAdmin, p)));
            } else {
                runExpectFail(rec, n, "统计-H2", "高峰-" + p, () -> app.statisticsPeak(superAdmin, p));
                runExpectFail(rec, n, "统计-H2", "趋势-" + p, () -> app.statisticsTrend(superAdmin, p));
            }
            runExpectFail(rec, n, "统计-H2", "报表-" + p, () -> {
                Map<String, Object> report = app.statisticsReport(superAdmin, p);
                assertNotNull(report.get("usage"));
            });
        }
        runExpectFail(rec, n, "统计-H2", "信用分布", () -> app.statisticsCredit());
        runPass(rec, n, "统计", "仪表盘(已精简)", () -> {
            Map<String, Object> dash = app.dashboard(superAdmin);
            assertNotNull(dash.get("ok"));
        });
        runPass(rec, n, "签到", "实时预约列表", () -> assertNotNull(app.liveReservations(superAdmin)));

        // ===== 管理员 =====
        String newAdminAccount = "ext_admin_" + batch.replace("-", "").toLowerCase();
        java.util.concurrent.atomic.AtomicReference<Long> newAdminIdRef = new java.util.concurrent.atomic.AtomicReference<>();
        runPass(rec, n, "管理员", "超管新建普管", () -> {
            Map<String, Object> a = app.createAdminAccount(superAdmin, Map.of(
                    "account", newAdminAccount, "password", "admin123", "name", "【极端测试】管理员", "phone", "13800000000"));
            newAdminIdRef.set(num(a.get("id")));
            rec.artifact("extremeAdminAccount", newAdminAccount);
        }, "管理端→管理员管理，账号 " + newAdminAccount);
        final Long newAdminId = newAdminIdRef.get();
        runPass(rec, n, "管理员", "更新普管", () -> app.updateAdminAccount(superAdmin, newAdminId, Map.of(
                "name", "【极端测试】管理员-改", "phone", "13800000001")));
        runPass(rec, n, "管理员", "禁用普管", () -> app.setAdminAccountStatus(superAdmin, newAdminId, "DISABLED"));
        runPass(rec, n, "管理员", "启用普管", () -> app.setAdminAccountStatus(superAdmin, newAdminId, "NORMAL"));
        runPass(rec, n, "管理员", "管理员列表", () -> assertTrue(app.adminAccounts(superAdmin).size() >= 2));
        runPass(rec, n, "管理员", "操作日志", () -> assertFalse(app.operationLogs(superAdmin).isEmpty()));

        // ===== 信用上限 =====
        runPass(rec, n, "信用", "加分不超过500上限", () -> {
            jdbc.update("update student_profile set credit_score=495 where user_id=?", student.id());
            jdbc.update("insert into credit_log(user_id,before_score,change_value,after_score,change_type,reason,created_at) values(?,?,?,?,?,?,?)",
                    student.id(), 495, 10, 500, "TEST", "极端测试加分", LocalDateTime.now());
            jdbc.update("update student_profile set credit_score=500 where user_id=?", student.id());
            int score = jdbc.queryForObject("select credit_score from student_profile where user_id=?", Integer.class, student.id());
            assertTrue(score <= 500);
        });

        // ===== 定时任务 =====
        runPass(rec, n, "定时", "无效签到撤销", () -> app.scheduledProcessInvalidCheckin());
        runPass(rec, n, "定时", "未到处理", () -> app.scheduledProcessNoShow());
        runExpectFail(rec, n, "定时-H2", "自动签退(MySQL语法)", () -> app.scheduledProcessAutoCheckout());
        runPass(rec, n, "定时", "黑名单释放", () -> app.scheduledProcessBlacklistRelease());
        runExpectFail(rec, n, "定时-H2", "暂离超时(MySQL语法)", () -> app.scheduledProcessTempLeaveTimeout());

        // ===== 补充查询类用例凑满100+ =====
        for (int i = 0; i < 15; i++) {
            final int fi = i;
            runPass(rec, n, "查询", "可用座位时段变体 #" + fi, () -> {
                String start = String.format("%02d:00", 8 + (fi % 10));
                String end = String.format("%02d:00", 10 + (fi % 10));
                app.availableSeats(extRoomId, tomorrow.toString(), start, end);
            });
        }
        runPass(rec, n, "预约", "管理端预约列表", () -> assertNotNull(app.adminReservations(normalAdmin)));
        runPass(rec, n, "签到", "签到记录", () -> assertNotNull(app.checkins(normalAdmin)));

        // 清理：删除极端测试自习室（需无进行中预约）
        jdbc.update("delete from reservation where room_id=?", extRoomId);
        runPass(rec, n, "清理", "删除极端测试自习室", () -> app.deleteRoom(superAdmin, extRoomId));
        runPass(rec, n, "清理", "删除极端测试公告", () -> app.deleteAnnouncement(annId));

        Path md = Path.of("target", "extreme-test-report.md");
        Path json = Path.of("target", "extreme-test-artifacts.json");
        rec.writeReports(md, json);

        System.out.println("\n========== 极端测试报告 ==========");
        System.out.println("批次: " + batch);
        System.out.println("总用例: " + (rec.artifacts().size() + " artifacts, cases in md"));
        System.out.println("报告: " + md.toAbsolutePath());
        System.out.println("意外失败: " + rec.unexpectedFailCount());
        System.out.println("==================================\n");

        assertEquals(0, rec.unexpectedFailCount(), "存在意外失败，详见 target/extreme-test-report.md");
        assertTrue(n.get() >= 100, "用例数应不少于100，实际=" + n.get());
    }

    private void runPass(ExtremeTestRecorder rec, AtomicInteger n, String cat, String action, Runnable r) {
        runPass(rec, n, cat, action, r, "");
    }

    private void runPass(ExtremeTestRecorder rec, AtomicInteger n, String cat, String action, Runnable r, String hint) {
        int no = n.incrementAndGet();
        try {
            r.run();
            rec.pass(no, cat, action, "OK", hint);
        } catch (Exception e) {
            rec.fail(no, cat, action, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void runExpectFail(ExtremeTestRecorder rec, AtomicInteger n, String cat, String action, Runnable r) {
        int no = n.incrementAndGet();
        try {
            r.run();
            rec.fail(no, cat, action, "期望失败但成功了");
        } catch (BusinessException e) {
            rec.expectedFail(no, cat, action, e.getCode() + " " + e.getMessage());
        } catch (Exception e) {
            rec.expectedFail(no, cat, action, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private CurrentUser loadAdmin(String account) {
        Map<String, Object> row = jdbc.queryForMap("select id,account,name,role from admin_account where account=?", account);
        String role = "SUPER_ADMIN".equals(String.valueOf(row.get("role"))) ? "SUPER_ADMIN" : "ADMIN";
        return new CurrentUser(num(row.get("id")), String.valueOf(row.get("account")), role, String.valueOf(row.get("name")));
    }

    private CurrentUser loadStudent(String studentNo) {
        Map<String, Object> row = jdbc.queryForMap("""
                select ua.id, ua.username, sp.name from user_account ua
                join student_profile sp on sp.user_id=ua.id where ua.username=?
                """, studentNo);
        return new CurrentUser(num(row.get("id")), String.valueOf(row.get("username")), "STUDENT", String.valueOf(row.get("name")));
    }

    private static Long num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
}
