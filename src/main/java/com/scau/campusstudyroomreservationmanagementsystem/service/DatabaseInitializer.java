package com.scau.campusstudyroomreservationmanagementsystem.service;

import org.springframework.beans.factory.annotation.Value;
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

    /** 每次启动将演示账号密码恢复为文档默认值，避免测试改密后无法登录。生产环境可设为 false。 */
    @Value("${app.demo.sync-accounts-on-startup:true}")
    private boolean syncAccountsOnStartup;

    public DatabaseInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createTables();
        migrateThirdDictionarySchema();
        seedData();
        if (syncAccountsOnStartup) {
            syncDemoAccounts();
        }
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
                  index idx_student_college_major(college,major),
                  constraint fk_student_profile_user foreign key(user_id) references user_account(id)
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
                  room_code varchar(10) not null unique,
                  name varchar(50) not null,
                  room_type varchar(10) not null default '普通',
                  location varchar(100) not null,
                  floor varchar(20) not null,
                  open_time time not null,
                  close_time time not null,
                  status varchar(20) not null,
                  manager_id bigint not null,
                  row_count int not null,
                  col_count int not null,
                  layout_image_url varchar(255) null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_room_manager(manager_id),
                  index idx_room_status(status),
                  constraint fk_study_room_manager foreign key(manager_id) references admin_account(id)
                ) charset=utf8mb4
                """,
                """
                create table if not exists facility(
                  id bigint primary key auto_increment,
                  name varchar(20) not null unique,
                  created_at datetime not null
                ) charset=utf8mb4
                """,
                """
                create table if not exists study_room_facility(
                  room_id bigint not null,
                  facility_id bigint not null,
                  primary key(room_id,facility_id),
                  constraint fk_room_facility_room foreign key(room_id) references study_room(id) on delete cascade,
                  constraint fk_room_facility_facility foreign key(facility_id) references facility(id)
                ) charset=utf8mb4
                """,
                """
                create table if not exists seat(
                  id bigint primary key auto_increment,
                  room_id bigint not null,
                  seat_no varchar(10) not null,
                  row_no int not null,
                  col_no int not null,
                  is_seat tinyint not null default 1,
                  cell_category varchar(20) not null,
                  seat_type varchar(10) not null,
                  has_power tinyint not null default 0,
                  near_window tinyint not null default 0,
                  quiet_zone tinyint not null default 0,
                  hot_seat tinyint not null default 0,
                  status varchar(20) not null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  unique key uk_room_seat(room_id,seat_no),
                  index idx_seat_room_status(room_id,status),
                  index idx_seat_feature(room_id,has_power,near_window,quiet_zone),
                  constraint fk_seat_room foreign key(room_id) references study_room(id) on delete cascade
                ) charset=utf8mb4
                """,
                """
                create table if not exists reservation(
                  id bigint primary key auto_increment,
                  reservation_no varchar(16) not null unique,
                  user_id bigint not null,
                  room_id bigint not null,
                  seat_id bigint not null,
                  reserve_date date not null,
                  start_time time not null,
                  end_time time not null,
                  status varchar(20) not null,
                  sign_in_time datetime null,
                  sign_out_time datetime null,
                  cancel_reason varchar(200) null,
                  created_at datetime not null,
                  updated_at datetime not null,
                  index idx_res_user_date(user_id,reserve_date),
                  index idx_res_room_date(room_id,reserve_date),
                  index idx_res_seat_date(seat_id,reserve_date),
                  index idx_res_status(status),
                  constraint fk_reservation_user foreign key(user_id) references user_account(id),
                  constraint fk_reservation_room foreign key(room_id) references study_room(id),
                  constraint fk_reservation_seat foreign key(seat_id) references seat(id)
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
                  unique key uk_seat_slot(seat_id,slot_start),
                  index idx_slot_reservation(reservation_id),
                  constraint fk_slot_reservation foreign key(reservation_id) references reservation(id) on delete cascade,
                  constraint fk_slot_seat foreign key(seat_id) references seat(id)
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
                  index idx_checkin_user_time(user_id,checkin_time),
                  constraint fk_checkin_reservation foreign key(reservation_id) references reservation(id) on delete cascade,
                  constraint fk_checkin_user foreign key(user_id) references user_account(id),
                  constraint fk_checkin_admin foreign key(admin_id) references admin_account(id)
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
                  index idx_credit_type(change_type),
                  constraint fk_credit_user foreign key(user_id) references user_account(id),
                  constraint fk_credit_reservation foreign key(reservation_id) references reservation(id)
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
                  index idx_blacklist_end_status(end_time,status),
                  constraint fk_blacklist_user foreign key(user_id) references user_account(id)
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
                  index idx_announcement_pinned(pinned),
                  constraint fk_announcement_room foreign key(room_id) references study_room(id),
                  constraint fk_announcement_publisher foreign key(publisher_id) references admin_account(id)
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
                  index idx_notification_user_time(user_id,created_at),
                  constraint fk_notification_user foreign key(user_id) references user_account(id)
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
                  index idx_feedback_user_time(user_id,created_at),
                  constraint fk_feedback_user foreign key(user_id) references user_account(id),
                  constraint fk_feedback_reservation foreign key(reservation_id) references reservation(id),
                  constraint fk_feedback_room foreign key(room_id) references study_room(id),
                  constraint fk_feedback_seat foreign key(seat_id) references seat(id),
                  constraint fk_feedback_handler foreign key(handler_id) references admin_account(id)
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
    }

    /**
     * 第三版数据字典迁移：中文枚举、房间设施拆表、派生字段改查询计算。
     */
    private void migrateThirdDictionarySchema() {
        migrateColumns();
        migrateEnumValues();
        migrateRoomFacilities();
        migrateReservationSlotSchema();
        dropLegacyDerivedColumns();
        createViews();
    }

    private void migrateColumns() {
        try {
            jdbc.execute("alter table study_room add column room_type varchar(10) not null default '普通' after name");
        } catch (Exception ignored) {
        }
        try {
            jdbc.execute("alter table study_room modify column room_code varchar(10) not null");
        } catch (Exception ignored) {
        }
        try {
            jdbc.execute("alter table reservation modify column reservation_no varchar(16) not null");
        } catch (Exception ignored) {
        }
        try {
            jdbc.execute("alter table seat modify column seat_no varchar(10) not null");
        } catch (Exception ignored) {
        }
    }

    private void migrateEnumValues() {
        runQuiet("update user_account set role='学生' where role='STUDENT'");
        runQuiet("update user_account set status='正常' where status='NORMAL'");
        runQuiet("update user_account set status='待审核' where status='PENDING'");
        runQuiet("update user_account set status='禁用' where status='DISABLED'");
        runQuiet("update user_account set status='黑名单' where status='BLACKLIST'");
        runQuiet("update student_profile set audit_status='待审核' where audit_status='PENDING'");
        runQuiet("update student_profile set audit_status='已通过' where audit_status='APPROVED'");
        runQuiet("update student_profile set audit_status='已拒绝' where audit_status='REJECTED'");
        runQuiet("update student_profile set grade=replace(grade,'级','') where grade like '____级'");
        runQuiet("update admin_account set role='普通管理员' where role='ADMIN'");
        runQuiet("update admin_account set role='超级管理员' where role='SUPER_ADMIN'");
        runQuiet("update admin_account set status='正常' where status='NORMAL'");
        runQuiet("update admin_account set status='离职' where status in ('DISABLED','禁用')");
        runQuiet("update study_room set status='开放' where status='OPEN'");
        runQuiet("update study_room set status='关闭' where status='CLOSED'");
        runQuiet("update study_room set status='维护中' where status in ('MAINTENANCE','MAINTAINING')");
        runQuiet("update seat set cell_category='座位' where cell_category='SEAT'");
        runQuiet("update seat set cell_category='非座位' where cell_category in ('NON_SEAT','AISLE')");
        runQuiet("update seat set seat_type='普通' where seat_type in ('普通座位','NORMAL','STANDARD')");
        runQuiet("update seat set seat_type='静音' where seat_type in ('QUIET','静音座位')");
        runQuiet("update seat set seat_type='舒适' where seat_type in ('COMFORT','精品座位')");
        runQuiet("update seat set status='空闲' where status='NORMAL'");
        runQuiet("update seat set status='维修' where status in ('DAMAGED','MAINTAINING')");
        runQuiet("update seat set status='停用' where status in ('DISABLED','禁用')");
        runQuiet("update reservation set status='待使用' where status='PENDING'");
        runQuiet("update reservation set status='使用中' where status='USING'");
        runQuiet("update reservation set status='已完成' where status in ('COMPLETED','AUTO_CHECKOUT')");
        runQuiet("update reservation set status='已取消' where status='CANCELLED'");
        runQuiet("update reservation set status='已违约' where status in ('VIOLATED','AUTO_CANCELLED')");
        runQuiet("update reservation_slot set status='占用' where status='ACTIVE'");
        runQuiet("update checkin_record set checkin_method='扫码签到' where checkin_method='QR_SCAN'");
        runQuiet("update checkin_record set checkin_method='学号签到' where checkin_method='STUDENT_NO'");
        runQuiet("update checkin_record set result='准时' where result='ON_TIME'");
        runQuiet("update credit_log set change_type='签到奖励' where change_type='ON_TIME_CHECKIN'");
        runQuiet("update credit_log set change_type='违约扣减' where change_type in ('NO_SHOW','USER_CANCEL')");
        runQuiet("update credit_log set change_type='系统恢复' where change_type in ('BLACKLIST_RELEASE','VIOLATION_REVOKE')");
        runQuiet("update credit_log set change_type='其他' where change_type in ('INVALID_CHECKIN_REVERT','NO_CHECKIN')");
        runQuiet("update blacklist_record set status='生效' where status='ACTIVE'");
        runQuiet("update blacklist_record set status='已解除' where status='RELEASED'");
        runQuiet("update announcement set type='系统通知' where type='SYSTEM'");
        runQuiet("update announcement set type='使用规则' where type='RULE'");
        runQuiet("update announcement set type='维护公告' where type='MAINTENANCE'");
        runQuiet("update announcement set status='已发布' where status='PUBLISHED'");
        runQuiet("update announcement set status='已删除' where status='DELETED'");
        runQuiet("update feedback_ticket set type='建议' where type='SUGGESTION'");
        runQuiet("update feedback_ticket set type='座位报修' where type='SEAT_REPAIR'");
        runQuiet("update feedback_ticket set type='环境' where type in ('ENVIRONMENT','NOISE')");
        runQuiet("update feedback_ticket set severity='低' where severity='LOW'");
        runQuiet("update feedback_ticket set severity='中' where severity='MEDIUM'");
        runQuiet("update feedback_ticket set severity='高' where severity='HIGH'");
        runQuiet("update feedback_ticket set severity='紧急' where severity='CRITICAL'");
        runQuiet("update feedback_ticket set status='待处理' where status in ('PENDING','PROCESSING')");
        runQuiet("update feedback_ticket set status='已处理' where status in ('DONE','CLOSED')");
        runQuiet("update notification_message set type='预约' where type in ('RESERVATION','CHECKIN','CHECKOUT')");
        runQuiet("update notification_message set type='公告' where type='ANNOUNCEMENT'");
        runQuiet("update notification_message set type='反馈' where type='FEEDBACK'");
        runQuiet("update notification_message set type='信用' where type in ('CREDIT','VIOLATION')");
        runQuiet("update notification_message set type='黑名单' where type='BLACKLIST'");
        runQuiet("update notification_message set type='系统' where type='SYSTEM'");
        runQuiet("update operation_log set module='认证' where module='AUTH'");
        runQuiet("update operation_log set module='用户' where module='USER'");
        runQuiet("update operation_log set module='自习室' where module='ROOM'");
        runQuiet("update operation_log set module='座位' where module='SEAT'");
        runQuiet("update operation_log set module='预约' where module='RESERVATION'");
        runQuiet("update operation_log set module='反馈' where module='FEEDBACK'");
        runQuiet("update operation_log set module='管理员' where module='ADMIN'");
        runQuiet("update operation_log set action='修改密码' where action='CHANGE_PASSWORD'");
        runQuiet("update operation_log set action='通过' where action='APPROVE'");
        runQuiet("update operation_log set action='拒绝' where action='REJECT'");
        runQuiet("update operation_log set action='新增' where action='CREATE'");
        runQuiet("update operation_log set action='更新' where action='UPDATE'");
        runQuiet("update operation_log set action='删除' where action='DELETE'");
        runQuiet("update operation_log set action='撤销违约' where action='REVOKE_VIOLATION'");
        runQuiet("update operation_log set action='处理' where action='HANDLE'");
        runQuiet("update operation_log set action='启用' where action='ENABLE'");
        runQuiet("update operation_log set action='禁用' where action='DISABLE'");
        runQuiet("update operation_log set target_type='学生' where target_type='STUDENT'");
        runQuiet("update operation_log set target_type='管理员' where target_type='ADMIN'");
        runQuiet("update operation_log set target_type='自习室' where target_type='STUDY_ROOM'");
        runQuiet("update operation_log set target_type='座位' where target_type='SEAT'");
        runQuiet("update operation_log set target_type='预约' where target_type='RESERVATION'");
        runQuiet("update operation_log set target_type='反馈' where target_type='FEEDBACK'");
        runQuiet("update operation_log set target_type='管理员账号' where target_type='ADMIN_ACCOUNT'");
        runQuiet("update operation_log set target_type='用户' where target_type='USER'");
    }

    private void migrateRoomFacilities() {
        for (String name : List.of("空调", "WiFi", "电源插座", "饮水机", "白板", "投影设备")) {
            upsertFacility(name);
        }
        if (!columnExists("study_room", "facilities")) {
            return;
        }
        List<java.util.Map<String, Object>> rows = jdbc.queryForList("select id, facilities from study_room");
        for (java.util.Map<String, Object> row : rows) {
            Long roomId = ((Number) row.get("id")).longValue();
            String raw = String.valueOf(row.getOrDefault("facilities", ""));
            for (String facility : raw.split("[,，、/\\s]+")) {
                String normalized = normalizeFacility(facility);
                if (normalized.isBlank()) {
                    continue;
                }
                upsertFacility(normalized);
                Long facilityId = jdbc.queryForObject("select id from facility where name=?", Long.class, normalized);
                runQuiet("insert ignore into study_room_facility(room_id,facility_id) values(" + roomId + "," + facilityId + ")");
            }
        }
    }

    private void migrateReservationSlotSchema() {
        runQuiet("delete from reservation_slot where status <> '占用'");
        try {
            jdbc.execute("alter table reservation_slot drop index uk_seat_slot");
        } catch (Exception ignored) {
        }
        try {
            jdbc.execute("alter table reservation_slot add unique key uk_seat_slot(seat_id, slot_start)");
        } catch (Exception ignored) {
        }
    }

    private void dropLegacyDerivedColumns() {
        runQuiet("drop view if exists v_room_daily_usage");
        dropColumn("reservation", "actual_minutes");
        dropColumn("study_room", "cell_count");
        dropColumn("study_room", "seat_count");
        dropColumn("study_room", "facilities");
    }

    private void dropColumn(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        try {
            jdbc.execute("alter table " + table + " drop column " + column);
        } catch (Exception ignored) {
        }
    }

    private boolean columnExists(String table, String column) {
        try {
            Integer count = jdbc.queryForObject("""
                    select count(*) from information_schema.columns
                    where lower(table_name)=lower(?) and lower(column_name)=lower(?)
                    """, Integer.class, table, column);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private void runQuiet(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception ignored) {
        }
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
                       count(distinct case when s.is_seat=1 then s.id end) seat_count,
                       count(distinct r.id) reservation_count,
                       count(distinct case when r.status in ('使用中','已完成') then r.id end) used_count,
                       round(if(count(distinct case when s.is_seat=1 then s.id end)=0,0,
                         count(distinct r.id)/count(distinct case when s.is_seat=1 then s.id end)*100),1) usage_rate
                from study_room sr
                left join seat s on s.room_id=sr.id
                left join reservation r on r.room_id=sr.id and r.reserve_date=current_date()
                group by sr.id, sr.name
                """);
        } catch (Exception ex) {
            if (!isH2Database()) {
                throw ex;
            }
            jdbc.execute("""
                create view if not exists v_room_daily_usage as
                select sr.id room_id,
                       sr.name room_name,
                       count(distinct case when s.is_seat=1 then s.id end) seat_count,
                       count(distinct r.id) reservation_count,
                       count(distinct case when r.status in ('使用中','已完成') then r.id end) used_count,
                       cast(count(distinct r.id) as decimal(10,1)) usage_rate
                from study_room sr
                left join seat s on s.room_id=sr.id
                left join reservation r on r.room_id=sr.id and r.reserve_date=current_date()
                group by sr.id, sr.name
                """);
        }
    }

    /**
     * 将内置演示账号恢复为文档约定密码与可登录状态（不删除业务数据）。
     * 学号 202301010101/102 → 123456；admin → admin123；superadmin → super123。
     */
    private void syncDemoAccounts() {
        LocalDateTime now = LocalDateTime.now();
        String studentPassword = passwordEncoder.encode("123456");
        String adminPassword = passwordEncoder.encode("admin123");
        String superPassword = passwordEncoder.encode("super123");

        for (String studentNo : List.of("202301010101", "202301010102")) {
            jdbc.update("""
                    update user_account set password_hash=?, status='正常', updated_at=?
                    where username=? and role='学生'
                    """, studentPassword, now, studentNo);
            jdbc.update("""
                    update student_profile set audit_status='已通过', updated_at=?
                    where student_no=?
                    """, now, studentNo);
        }
        jdbc.update("""
                update user_account set password_hash=?, status='待审核', updated_at=?
                where username='202301010199' and role='学生'
                """, studentPassword, now);

        Long demoStudentId = jdbc.queryForObject(
                "select id from user_account where username='202301010101' and role='学生'", Long.class);
        if (demoStudentId != null) {
            jdbc.update("update student_profile set credit_score=280, updated_at=? where user_id=?", now, demoStudentId);
            jdbc.update("""
                    update blacklist_record set status='已解除', released_at=?
                    where user_id=? and status='生效'
                    """, now, demoStudentId);
        }

        jdbc.update("""
                update admin_account set password_hash=?, status='正常', updated_at=?
                where account='admin'
                """, adminPassword, now);
        jdbc.update("""
                update admin_account set password_hash=?, status='正常', updated_at=?
                where account='superadmin'
                """, superPassword, now);
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
                "202301010101", studentPassword, "学生", "正常", now, now);
        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                "202301010102", studentPassword, "学生", "正常", now, now);
        jdbc.update("insert into user_account(username,password_hash,role,status,created_at,updated_at) values(?,?,?,?,?,?)",
                "202301010199", studentPassword, "学生", "待审核", now, now);
        Long s1 = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long s2 = jdbc.queryForObject("select id from user_account where username='202301010102'", Long.class);
        Long s3 = jdbc.queryForObject("select id from user_account where username='202301010199'", Long.class);
        insertProfile(s1, "202301010101", "张三", "已通过", 280);
        insertProfile(s2, "202301010102", "李四", "已通过", 320);
        insertProfile(s3, "202301010199", "王五", "待审核", 300);

        jdbc.update("insert into admin_account(account,password_hash,name,role,phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                "admin", adminPassword, "张老师", "普通管理员", "13800138000", "正常", now, now);
        jdbc.update("insert into admin_account(account,password_hash,name,role,phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?)",
                "superadmin", superPassword, "超级管理员", "超级管理员", "13900139000", "正常", now, now);
        Long adminId = jdbc.queryForObject("select id from admin_account where account='admin'", Long.class);
        Long superId = jdbc.queryForObject("select id from admin_account where account='superadmin'", Long.class);

        insertRoom("LIB01A", "图书馆一楼A区", "普通", "图书馆1楼东侧", "1楼", "07:00:00", "22:30:00", "开放", adminId, 4, 6, "空调,WiFi,饮水机,电源插座");
        insertRoom("LIB02B", "图书馆二楼B区", "静音", "图书馆2楼西侧", "2楼", "07:00:00", "22:30:00", "开放", adminId, 4, 6, "空调,WiFi,电源插座");
        insertRoom("TEAC201", "教学楼C201", "讨论", "教学楼C栋2楼", "2楼", "08:00:00", "21:00:00", "维护中", superId, 3, 6, "空调,WiFi,投影设备,白板");

        jdbc.update("insert into announcement(title,content,type,pinned,scope,publisher_id,status,published_at,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?)",
                "期末周开放时间延长", "图书馆自习室期末周开放至 22:30，请同学们按预约时段有序使用。", "系统通知", 1, "全体", adminId, "已发布", now.minusDays(1), now.minusDays(1), now);
        jdbc.update("insert into announcement(title,content,type,pinned,scope,publisher_id,status,published_at,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?)",
                "签到规则提醒", "预约开始前后 15 分钟内可由管理员扫码签到，超时未签到将扣除信用积分。", "使用规则", 0, "全体", adminId, "已发布", now.minusHours(5), now.minusHours(5), now);

        Long room1 = jdbc.queryForObject("select id from study_room where room_code='LIB01A'", Long.class);
        Long room2 = jdbc.queryForObject("select id from study_room where room_code='LIB02B'", Long.class);
        Long seat1 = jdbc.queryForObject("select id from seat where room_id=? and seat_no='A-012'", Long.class, room1);
        Long seat2 = jdbc.queryForObject("select id from seat where room_id=? and seat_no='B-003'", Long.class, room2);
        String pendingNo = LocalDate.now().toString().replace("-", "") + "00000001";
        String completedNo = LocalDate.now().minusDays(1).toString().replace("-", "") + "00000002";
        jdbc.update("insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,sign_in_time,sign_out_time,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                pendingNo, s1, room1, seat1, LocalDate.now(), "09:00:00", "11:00:00", "待使用", null, null, now.minusHours(1), now);
        jdbc.update("insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,sign_in_time,sign_out_time,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                completedNo, s1, room2, seat2, LocalDate.now().minusDays(1), "14:00:00", "16:00:00", "已完成", now.minusDays(1).withHour(14), now.minusDays(1).withHour(16), now.minusDays(2), now.minusDays(1));
        Long reservation = jdbc.queryForObject("select id from reservation where reservation_no=?", Long.class, pendingNo);
        jdbc.update("insert into notification_message(user_id,title,content,type,related_id,created_at) values(?,?,?,?,?,?)",
                s1, "预约即将开始", "你的图书馆一楼A区 A-012 座位预约请按时签到。", "预约", reservation, now.minusMinutes(15));
        addCredit(s1, 300, -20, 280, "违约扣减", "历史未按时签到扣分", null, now.minusDays(3));
        jdbc.update("insert into feedback_ticket(user_id,room_id,seat_id,type,severity,content,status,created_at) values(?,?,?,?,?,?,?,?)",
                s1, room1, seat1, "座位报修", "中", "A-012 插座接触不良，请安排维修。", "待处理", now.minusHours(2));
    }

    private void insertProfile(Long userId, String studentNo, String name, String audit, int credit) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into student_profile(user_id,student_no,name,gender,college,major,grade,phone,email,material_url,audit_status,credit_score,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                userId, studentNo, name, "男", "计算机科学与技术学院", "软件工程", "2023", "13152940518", studentNo + "@campus.edu.cn", "/uploads/material/demo.pdf", audit, credit, now, now);
    }

    private void insertRoom(String code, String name, String roomType, String location, String floor, String open, String close,
                            String status, Long managerId, int rows, int cols, String facilities) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("insert into study_room(room_code,name,room_type,location,floor,open_time,close_time,status,manager_id,row_count,col_count,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                code, name, roomType, location, floor, open, close, status, managerId, rows, cols, now, now);
        Long roomId = jdbc.queryForObject("select id from study_room where room_code=?", Long.class, code);
        upsertRoomFacilities(roomId, facilities);
        String prefix = code.contains("02") ? "B" : code.contains("C") ? "C" : "A";
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                boolean aisle = (r == rows && (c == 2 || c == 5));
                String no = prefix + "-" + String.format("%03d", (r - 1) * cols + c);
                jdbc.update("insert into seat(room_id,seat_no,row_no,col_no,is_seat,cell_category,seat_type,has_power,near_window,quiet_zone,hot_seat,status,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        roomId, no, r, c, aisle ? 0 : 1, aisle ? "非座位" : "座位", aisle ? "普通" : (r <= 2 ? "静音" : "普通"),
                        c % 2 == 0 ? 1 : 0, c == 1 || c == cols ? 1 : 0, r <= 2 ? 1 : 0, c == 3 ? 1 : 0,
                        aisle ? "停用" : "空闲", now, now);
            }
        }
    }

    private void addCredit(Long userId, int before, int delta, int after, String type, String reason, Long reservationId, LocalDateTime at) {
        jdbc.update("insert into credit_log(user_id,before_score,change_value,after_score,change_type,reason,reservation_id,created_at) values(?,?,?,?,?,?,?,?)",
                userId, before, delta, after, type, reason, reservationId, at);
    }

    private void upsertRoomFacilities(Long roomId, String facilities) {
        for (String item : facilities.split("[,，、/\\s]+")) {
            String name = normalizeFacility(item);
            if (name.isBlank()) {
                continue;
            }
            upsertFacility(name);
            Long facilityId = jdbc.queryForObject("select id from facility where name=?", Long.class, name);
            try {
                jdbc.update("insert into study_room_facility(room_id,facility_id) values(?,?)", roomId, facilityId);
            } catch (Exception ignored) {
            }
        }
    }

    private void upsertFacility(String name) {
        try {
            jdbc.update("insert into facility(name,created_at) values(?,?)", name, LocalDateTime.now());
        } catch (Exception ignored) {
        }
    }

    private String normalizeFacility(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return "";
        }
        return switch (text) {
            case "充电区", "插座", "电源" -> "电源插座";
            case "投影仪" -> "投影设备";
            default -> text;
        };
    }
}
