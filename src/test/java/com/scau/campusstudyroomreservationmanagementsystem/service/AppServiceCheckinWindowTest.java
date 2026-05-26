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
    void qrCodeRejectsOutsideCheckinWindow() {
        Long userId = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        Long seatId = jdbc.queryForObject("select id from seat limit 1", Long.class);
        Long roomId = jdbc.queryForObject("select room_id from seat where id=?", Long.class, seatId);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?)
                """, "TEST-QR-WINDOW", userId, roomId, seatId, Date.valueOf(tomorrow),
                Time.valueOf(LocalTime.of(19, 0)), Time.valueOf(LocalTime.of(21, 0)),
                "PENDING", LocalDateTime.now(), LocalDateTime.now());

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
                """, "TEST-INVALID-CHECKIN", userId, roomId, seatId, Date.valueOf(today),
                Time.valueOf(LocalTime.of(19, 0)), Time.valueOf(LocalTime.of(21, 0)),
                "USING", signIn, LocalDateTime.now(), LocalDateTime.now());
        Long reservationId = jdbc.queryForObject(
                "select id from reservation where reservation_no='TEST-INVALID-CHECKIN'", Long.class);
        jdbc.update("""
                insert into checkin_record(reservation_id,user_id,admin_id,checkin_method,checkin_time,result)
                values(?,?,?,?,?,?)
                """, reservationId, userId, 1L, "QR_SCAN", signIn, "ON_TIME");

        appService.scheduledProcessInvalidCheckin();

        String status = jdbc.queryForObject(
                "select status from reservation where id=?", String.class, reservationId);
        Integer checkinCount = jdbc.queryForObject(
                "select count(*) from checkin_record where reservation_id=?", Integer.class, reservationId);
        assertEquals("PENDING", status);
        assertEquals(0, checkinCount);
    }
}
