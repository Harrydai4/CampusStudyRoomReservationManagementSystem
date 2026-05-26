package com.scau.campusstudyroomreservationmanagementsystem.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.sql.Date;
import java.sql.Time;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务业务逻辑测试：使用 H2 内存库，无需本机 MySQL。
 */
@SpringBootTest
@ActiveProfiles("test")
class AppServiceScheduledTest {
    @Autowired
    private AppService appService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void scheduledProcessNoShowMarksViolatedAndDeductsCredit() {
        Long userId = jdbc.queryForObject("select id from user_account where username='202301010101'", Long.class);
        int before = jdbc.queryForObject("select credit_score from student_profile where user_id=?", Integer.class, userId);
        Long seatId = jdbc.queryForObject("select id from seat limit 1", Long.class);
        Long roomId = jdbc.queryForObject("select room_id from seat where id=?", Long.class, seatId);
        LocalDate today = LocalDate.now();
        LocalTime start = LocalTime.now().minusMinutes(30);
        LocalTime end = start.plusHours(2);
        jdbc.update("""
                insert into reservation(reservation_no,user_id,room_id,seat_id,reserve_date,start_time,end_time,status,created_at,updated_at)
                values(?,?,?,?,?,?,?,?,?,?)
                """, "TEST-NOSHOW-001", userId, roomId, seatId, Date.valueOf(today), Time.valueOf(start), Time.valueOf(end),
                "PENDING", LocalDateTime.now(), LocalDateTime.now());

        appService.scheduledProcessNoShow();

        Integer violated = jdbc.queryForObject(
                "select count(*) from reservation where reservation_no='TEST-NOSHOW-001' and status='VIOLATED'", Integer.class);
        int after = jdbc.queryForObject("select credit_score from student_profile where user_id=?", Integer.class, userId);
        assertEquals(1, violated);
        assertTrue(after < before);
    }
}
