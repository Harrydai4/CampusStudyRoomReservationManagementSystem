package com.scau.campusstudyroomreservationmanagementsystem.service;

import com.scau.campusstudyroomreservationmanagementsystem.support.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 签到时间窗与无效签到撤销测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AppServiceCheckinWindowTest {
    @Autowired
    private AppService appService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void scanCheckinByStudentNoWithinWindow() {
        Long userId = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long seatId = jdbc.queryForObject("select id from seat where is_seat=1 limit 1", Long.class);
        Long roomId = jdbc.queryForObject("select room_id from seat where id=?", Long.class, seatId);
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now().withSecond(0).withNano(0);
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?)
                """, today.toString().replace("-", "") + "90000001", userId, roomId, seatId, Date.valueOf(today),
                Time.valueOf(nowTime), Time.valueOf(nowTime.plusHours(2)),
                "待使用", LocalDateTime.now(), LocalDateTime.now());

        var admin = new com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser(
                1L, "admin", "ADMIN", "张老师");
        var result = appService.scanCheckin(admin, Map.of("studentNo", "202301010101"));
        assertEquals("使用中", result.get("status"));
    }

    @Test
    void qrCodeRejectsOutsideCheckinWindow() {
        Long userId = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long seatId = jdbc.queryForObject("select id from seat limit 1", Long.class);
        Long roomId = jdbc.queryForObject("select room_id from seat where id=?", Long.class, seatId);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?)
                """, tomorrow.toString().replace("-", "") + "90000002", userId, roomId, seatId, Date.valueOf(tomorrow),
                Time.valueOf(LocalTime.of(19, 0)), Time.valueOf(LocalTime.of(21, 0)),
                "待使用", LocalDateTime.now(), LocalDateTime.now());

        var user = new com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser(
                userId, "202301010101", "STUDENT", "张三");
        BusinessException ex = assertThrows(BusinessException.class, () -> appService.qrCode(user));
        assertTrue(ex.getMessage().contains("不在签到时间内"));
    }

    @Test
    void scheduledProcessInvalidCheckinRevertsOutsideWindowSignIn() {
        Long userId = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long seatId = jdbc.queryForObject("select id from seat limit 1", Long.class);
        Long roomId = jdbc.queryForObject("select room_id from seat where id=?", Long.class, seatId);
        LocalDate today = LocalDate.now();
        LocalDateTime signIn = LocalDateTime.of(today, LocalTime.of(8, 0));
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,sign_in_time,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?,?)
                """, today.toString().replace("-", "") + "90000003", userId, roomId, seatId, Date.valueOf(today),
                Time.valueOf(LocalTime.of(19, 0)), Time.valueOf(LocalTime.of(21, 0)),
                "使用中", signIn, LocalDateTime.now(), LocalDateTime.now());
        Long reservationId = jdbc.queryForObject(
                "select id from reservation where reservation_no=?", Long.class, today.toString().replace("-", "") + "90000003");
        jdbc.update("""
                insert into checkin_record(reservation_id,user_id,admin_id,checkin_method,checkin_time,result)
                values(?,?,?,?,?,?)
                """, reservationId, userId, 1L, "扫码签到", signIn, "准时");

        appService.scheduledProcessInvalidCheckin();

        String status = jdbc.queryForObject(
                "select status from reservation where id=?", String.class, reservationId);
        Integer checkinCount = jdbc.queryForObject(
                "select count(*) from checkin_record where reservation_id=?", Integer.class, reservationId);
        assertEquals("待使用", status);
        assertEquals(0, checkinCount);
    }
}
