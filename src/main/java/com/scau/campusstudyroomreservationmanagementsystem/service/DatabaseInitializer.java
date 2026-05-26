package com.scau.campusstudyroomreservationmanagementsystem.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.sql.Connection;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    /** 缓存数据库类型，避免重复打开连接导致连接池耗尽。 */
    private Boolean h2Database;

    public DatabaseInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createTables();
        seedData();
    }

    private void createTables() {
        List<String> ddl = List.of(
                """
                create table if not exists user_account(
                  id bigint primary key auto_increment,
                  username varchar(30) not null unique,
                  password_hash varchar(100) not null,
                  role varchar(20) not null,
                  status varchar(20) not null,
                  last_login_at datetime null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_user_role_status(role,status)
                ) charset=utf8mb4
                """,
                """
                create table if not exists student_profile(
                  id bigint primary key auto_increment,
                  user_id bigint not null unique,
                  student_no varchar(20) not null unique,
                  name varchar(50) not null,
                  gender varchar(10) not null,
                  college varchar(50) not null,
                  major varchar(50) not null,
                  grade varchar(10) not null,
                  phone varchar(20) not null,
                  email varchar(100) not null,
                  material_url varchar(255) null,
                  audit_status varchar(20) not null,
                  audit_remark varchar(255) null,
                  credit_score int not null default 300,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_student_audit_status(audit_status),
                  index idx_student_college_major(college,major)
                ) charset=utf8mb4
                """,
                """
                create table if not exists admin_account(
                  id bigint primary key auto_increment,
                  account varchar(30) not null unique,
                  password_hash varchar(100) not null,
                  name varchar(20) not null,
                  role varchar(20) not null,
                  phone varchar(20) null,
                  status varchar(20) not null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_admin_role(role)
                ) charset=utf8mb4
                """,
                """
                create table if not exists study_room(
                  id bigint primary key auto_increment,
                  room_code varchar(20) not null unique,
                  name varchar(50) not null,
                  location varchar(100) not null,
                  floor varchar(20) not null,
                  open_time time not null,
                  close_time time not null,
                  status varchar(20) not null,
                  manager_id bigint not null,
                  row_count int not null,
                  col_count int not null,
                  cell_count int not null,
                  seat_count int not null,
                  facilities varchar(200) null,
                  layout_image_url varchar(255) null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_room_manager(manager_id),
                  index idx_room_status(status)
                ) charset=utf8mb4
                """,
                """
                create table if not exists seat(
                  id bigint primary key auto_increment,
                  room_id bigint not null,
                  seat_no varchar(20) not null,
                  row_no int not null,
                  col_no int not null,
                  is_seat tinyint not null default 1,
                  cell_category varchar(20) not null,
                  seat_type varchar(80) not null,
                  has_power tinyint not null default 0,
                  near_window tinyint not null default 0,
                  quiet_zone tinyint not null default 0,
                  hot_seat tinyint not null default 0,
                  status varchar(20) not null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  unique key uk_room_seat(room_id,seat_no),
                  index idx_seat_room_status(room_id,status),
                  index idx_seat_feature(room_id,has_power,near_window,quiet_zone)
                ) charset=utf8mb4
                """,
                """
                create table if not exists reservation(
                  id bigint primary key auto_increment,
                  reservation_no varchar(30) not null unique,
                  user_id bigint not null,
                  room_id bigint not null,
                  seat_id bigint not null,
                  reserve_date date not null,
                  start_time time not null,
                  end_time time not null,
                  status varchar(20) not null,
                  sign_in_time datetime null,
                  sign_out_time datetime null,
                  actual_minutes int not null default 0,
                  cancel_reason varchar(200) null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_res_user_date(user_id,reserve_date),
                  index idx_res_room_date(room_id,reserve_date),
                  index idx_res_seat_date(seat_id,reserve_date),
                  index idx_res_status(status)
                ) charset=utf8mb4
                """,
                """
                create table if not exists reservation_slot(
                  id bigint primary key auto_increment,
                  reservation_id bigint not null,
                  seat_id bigint not null,
                  slot_start datetime not null,
                  slot_end datetime not null,
                  status varchar(20) not null,
                  unique key uk_seat_slot(seat_id,slot_start,status),
                  index idx_slot_reservation(reservation_id)
                ) charset=utf8mb4
                """,
                """
                create table if not exists checkin_record(
                  id bigint primary key auto_increment,
                  reservation_id bigint not null unique,
                  user_id bigint not null,
                  admin_id bigint not null,
                  checkin_method varchar(20) not null,
                  checkin_time datetime not null,
                  checkout_time datetime null,
                  result varchar(20) not null,
                  remark varchar(200) null,
                  index idx_checkin_admin_time(admin_id,checkin_time),
                  index idx_checkin_user_time(user_id,checkin_time)
                ) charset=utf8mb4
                """,
                """
                create table if not exists credit_log(
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  before_score int not null,
                  change_value int not null,
                  after_score int not null,
                  change_type varchar(30) not null,
                  reason varchar(100) not null,
                  reservation_id bigint null,
                  created_at datetime not null,
                  index idx_credit_user_time(user_id,created_at),
                  index idx_credit_type(change_type)
                ) charset=utf8mb4
                """,
                """
                create table if not exists blacklist_record(
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  start_time datetime not null,
                  end_time datetime not null,
                  reason varchar(200) not null,
                  status varchar(20) not null,
                  released_at datetime null,
                  index idx_blacklist_user_status(user_id,status),
                  index idx_blacklist_end_status(end_time,status)
                ) charset=utf8mb4
                """,
                """
                create table if not exists announcement(
                  id bigint primary key auto_increment,
                  title varchar(100) not null,
                  content text not null,
                  type varchar(20) not null,
                  pinned tinyint not null default 0,
                  scope varchar(20) not null,
                  room_id bigint null,
                  publisher_id bigint not null,
                  status varchar(20) not null,
                  published_at datetime null,
                  view_count int not null default 0,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_announcement_status_time(status,published_at),
                  index idx_announcement_room(room_id),
                  index idx_announcement_pinned(pinned)
                ) charset=utf8mb4
                """,
                """
                create table if not exists notification_message(
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  title varchar(100) not null,
                  content varchar(500) not null,
                  type varchar(20) not null,
                  read_flag tinyint not null default 0,
                  related_id bigint null,
                  created_at datetime not null,
                  read_at datetime null,
                  index idx_notification_user_read(user_id,read_flag),
                  index idx_notification_user_time(user_id,created_at)
                ) charset=utf8mb4
                """,
                """
                create table if not exists feedback_ticket(
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  reservation_id bigint null,
                  room_id bigint null,
                  seat_id bigint null,
                  type varchar(30) not null,
                  severity varchar(20) not null,
                  content varchar(1000) not null,
                  status varchar(20) not null,
                  handler_id bigint null,
                  handle_result varchar(1000) null,
                  created_at datetime not null,
                  handled_at datetime null,
                  index idx_feedback_status(status),
                  index idx_feedback_room_status(room_id,status),
                  index idx_feedback_user_time(user_id,created_at)
                ) charset=utf8mb4
                """,
                """
                create table if not exists temp_leave(
                  id bigint primary key auto_increment,
                  reservation_id bigint not null,
                  user_id bigint not null,
                  leave_time datetime not null,
                  return_time datetime null,
                  leave_status varchar(20) not null,
                  max_leave_minutes int not null default 30,
                  created_at datetime not null,
                  index idx_temp_leave_res(reservation_id,leave_status),
                  index idx_temp_leave_user(user_id,leave_time)
                ) charset=utf8mb4
                """,
                """
                create table if not exists operation_log(
                  id bigint primary key auto_increment,
                  operator_id bigint not null,
                  operator_name varchar(50) not null,
                  module varchar(30) not null,
                  action varchar(30) not null,
                  target_type varchar(30) null,
                  target_id bigint null,
                  detail varchar(500) null,
                  created_at datetime not null,
                  index idx_op_log_operator(operator_id,created_at),
                  index idx_op_log_module(module,created_at)
                ) charset=utf8mb4
                """
        );
        ddl.forEach(sql -> jdbc.execute(adaptDdl(sql)));
        createViews();
    }

    /** H2 测试库不支持 charset=utf8mb4，需剥离该子句。 */
    private String adaptDdl(String sql) {
        if (!isH2Database()) {
            return sql;
        }
        return sql.replace(" charset=utf8mb4", "");
    }

    private boolean isH2Database() {
        if (h2Database != null) {
            return h2Database;
        }
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            h2Database = "H2".equalsIgnoreCase(conn.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            h2Database = false;
        }
        return h2Database;
    }

    private void createViews() {
        try {
            if (isH2Database()) {
                jdbc.execute("drop view if exists v_room_daily_usage");
            }
            jdbc.execute("""
                create or replace view v_room_daily_usage as
                select sr.id room_id,
                       sr.name room_name,
                       sr.seat_count,
                       count(r.id) reservation_count,
                       sum(case when r.status in ('USING','COMPLETED','AUTO_CHECKOUT','TEMP_LEAVE') then 1 else 0 end) used_count,
                       round(if(sr.seat_count=0,0,count(r.id)/sr.seat_count*100),1) usage_rate
                from study_room sr
                left join reservation r on r.room_id=sr.id and r.reserve_date=current_date()
                group by sr.id, sr.name, sr.seat_count
                """);
        } catch (Exception ex) {
            if (!isH2Database()) {
                throw ex;
            }
            jdbc.execute("""
                create view if not exists v_room_daily_usage as
                select sr.id room_id,
                       sr.name room_name,
                       sr.seat_count,
                       count(r.id) reservation_count,
                       sum(case when r.status in ('USING','COMPLETED','AUTO_CHECKOUT','TEMP_LEAVE') then 1 else 0 end) used_count,
                       cast(count(r.id) as decimal(10,1)) usage_rate
                from study_room sr
                left join reservation r on r.room_id=sr.id and r.reserve_date=current_date()
                group by sr.id, sr.name, sr.seat_count
                """);
        }
    }

    private void seedData() {
        Integer admins = jdbc.queryForObject("select count(*) from admin_account", Integer.class);
        if (admins != null && admins > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String studentPassword = passwordEncoder.encode("123456");
        String adminPassword = passwordEncoder.encode("admin123");
        String superPassword = passwordEncoder.encode("super123");

        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                "202301010101", studentPassword, "STUDENT", "NORMAL", now, now);
        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                "202301010102", studentPassword, "STUDENT", "NORMAL", now, now);
        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                "202301010199", studentPassword, "STUDENT", "PENDING", now, now);
        Long s1 = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long s2 = jdbc.queryForObject("select id from user_account where username='202301010102'", Long.class);
        Long s3 = jdbc.queryForObject("select id from user_account where username='202301010199'", Long.class);
        insertProfile(s1, "202301010101", "张三", "APPROVED", 280);
        insertProfile(s2, "202301010102", "李四", "APPROVED", 320);
        insertProfile(s3, "202301010199", "待审同学", "PENDING", 300);

        jdbc.update("insert into admin_account(account,password_hash,name,role,phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                "admin", adminPassword, "张老师", "ADMIN", "13800138000", "NORMAL", now, now);
        jdbc.update("insert into admin_account(account,password_hash,name,role,phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                "superadmin", superPassword, "超级管理员", "SUPER_ADMIN", "13900139000", "NORMAL", now, now);
        Long adminId = jdbc.queryForObject("select id from admin_account where account='admin'", Long.class);
        Long superId = jdbc.queryForObject("select id from admin_account where account='superadmin'", Long.class);

        insertRoom("LIB-01-A", "图书馆一楼A区", "图书馆1楼东侧", "1楼", "07:00:00", "22:30:00", "OPEN", adminId, 4, 6, "空调,WiFi,饮水机,充电区");
        insertRoom("LIB-02-B", "图书馆二楼B区", "图书馆2楼西侧", "2楼", "07:00:00", "22:30:00", "OPEN", adminId, 4, 6, "空调,WiFi,打印区");
        insertRoom("TEA-C201", "教学楼C201", "教学楼C栋2楼", "2楼", "08:00:00", "21:00:00", "MAINTAINING", superId, 3, 6, "空调,WiFi,投影仪");

        jdbc.update("insert into announcement(title,content,type,pinned,scope,publisher_id,status,published_at,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?)",
                "期末周开放时间延长", "图书馆自习室期末周开放至 22:30，请同学们按预约时段有序使用。", "SYSTEM", 1, "GLOBAL", adminId, "PUBLISHED", now.minusDays(1), now.minusDays(1), now);
        jdbc.update("insert into announcement(title,content,type,pinned,scope,publisher_id,status,published_at,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?)",
                "签到规则提醒", "预约开始前后 15 分钟内可由管理员扫码签到，超时未签到将扣除信用积分。", "RULE", 0, "GLOBAL", adminId, "PUBLISHED", now.minusHours(5), now.minusHours(5), now);

        Long room1 = jdbc.queryForObject("select id from study_room where room_code='LIB-01-A'", Long.class);
        Long room2 = jdbc.queryForObject("select id from study_room where room_code='LIB-02-B'", Long.class);
        Long seat1 = jdbc.queryForObject("select id from seat where room_id=? and seat_no='A-12'", Long.class, room1);
        Long seat2 = jdbc.queryForObject("select id from seat where room_id=? and seat_no='B-03'", Long.class, room2);
        jdbc.update("insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,sign_in_time,sign_out_time,actual_minutes,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "R" + LocalDate.now().toString().replace("-", "") + "0001", s1, room1, seat1, LocalDate.now(), "09:00:00", "11:00:00", "PENDING", null, null, 0, now.minusHours(1), now);
        jdbc.update("insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,sign_in_time,sign_out_time,actual_minutes,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                "R" + LocalDate.now().minusDays(1).toString().replace("-", "") + "0002", s1, room2, seat2, LocalDate.now().minusDays(1), "14:00:00", "16:00:00", "COMPLETED", now.minusDays(1).withHour(14), now.minusDays(1).withHour(16), 120, now.minusDays(2), now.minusDays(1));
        Long reservation = jdbc.queryForObject("select id from reservation where reservation_no=?", Long.class, "R" + LocalDate.now().toString().replace("-", "") + "0001");
        jdbc.update("insert into notification_message(user_id,title,content,type,related_id,created_at) values(?,?,?,?,?,?)",
                s1, "预约即将开始", "你的图书馆一楼A区 A-12 座位预约请按时签到。", "RESERVATION", reservation, now.minusMinutes(15));
        addCredit(s1, 300, -20, 280, "NO_CHECKIN", "历史未按时签到扣分", null, now.minusDays(3));
        jdbc.update("insert into feedback_ticket(user_id,room_id,seat_id,type,severity,content,status,created_at) values(?,?,?,?,?,?,?,?)",
                s1, room1, seat1, "SEAT_REPAIR", "MEDIUM", "A-12 插座接触不良，请安排维修。", "PENDING", now.minusHours(2));
    }

    private void insertProfile(Long userId, String studentNo, String name, String audit, int credit) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into student_profile(user_id,student_no,name,gender,college,major,grade,phone,email,material_url,audit_status,credit_score,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                userId, studentNo, name, "男", "计算机科学与技术学院", "软件工程", "2023级", "13152940518", studentNo + "@campus.edu.cn", "/uploads/material/demo.pdf", audit, credit, now, now);
    }

    private void insertRoom(String code, String name, String location, String floor, String open, String close,
                            String status, Long managerId, int rows, int cols, String facilities) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into study_room(room_code,name,location,floor,open_time,close_time,status,manager_id,row_count,col_count,cell_count,seat_count,facilities,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                code, name, location, floor, open, close, status, managerId, rows, cols, rows * cols, rows * cols - 2, facilities, now, now);
        Long roomId = jdbc.queryForObject("select id from study_room where room_code=?", Long.class, code);
        String prefix = code.contains("02") ? "B" : code.contains("C") ? "C" : "A";
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                boolean aisle = (r == rows && (c == 2 || c == 5));
                String no = prefix + "-" + String.format("%02d", (r - 1) * cols + c);
                jdbc.update("insert into seat(room_id,seat_no,row_no,col_no,is_seat,cell_category,seat_type,has_power,near_window,quiet_zone,hot_seat,status,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        roomId, no, r, c, aisle ? 0 : 1, aisle ? "NON_SEAT" : "SEAT", aisle ? "过道" : "普通座位",
                        c % 2 == 0 ? 1 : 0, c == 1 || c == cols ? 1 : 0, r <= 2 ? 1 : 0, c == 3 ? 1 : 0,
                        aisle ? "DISABLED" : "NORMAL", now, now);
            }
        }
    }

    private void addCredit(Long userId, int before, int delta, int after, String type, String reason, Long reservationId, LocalDateTime at) {
        jdbc.update("insert into credit_log(user_id,before_score,change_value,after_score,change_type,reason,reservation_id,created_at) values(?,?,?,?,?,?,?,?)",
                userId, before, delta, after, type, reason, reservationId, at);
    }
}
