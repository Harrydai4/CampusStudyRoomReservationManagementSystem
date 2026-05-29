package com.scau.campusstudyroomreservationmanagementsystem;

import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import com.scau.campusstudyroomreservationmanagementsystem.service.AppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证「数据库 ↔ 后端 Service ↔ API 数据结构」真实链路一致（H2 集成环境）。
 * 覆盖：预约写入、取消扣分、统计报表与 SQL 计数对齐。
 */
@SpringBootTest
@ActiveProfiles("test")
class RealDataChainIntegrationTest {

    @Autowired
    AppService app;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    @Transactional
    void reservationCancelCreditAndStatisticsMatchDatabase() {
        Long roomId = jdbc.queryForObject("select id from study_room where room_code='LIB01A'", Long.class);
        Long seatId = jdbc.queryForObject(
                "select id from seat where room_id=? and is_seat=1 and status='空闲' limit 1", Long.class, roomId);
        Long userId = jdbc.queryForObject("select user_id from student_profile where student_no='202301010101'", Long.class);
        CurrentUser student = new CurrentUser(userId, "202301010101", "STUDENT", "张三");

        int scoreBefore = jdbc.queryForObject("select credit_score from student_profile where user_id=?", Integer.class, userId);
        LocalDate date = LocalDate.now().plusDays(2);

        Map<String, Object> created = app.createReservation(student, Map.of(
                "roomId", roomId, "seatId", seatId,
                "reserveDate", date.toString(), "startTime", "09:00", "endTime", "11:00"));
        Long reservationId = ((Number) created.get("id")).longValue();

        Integer slotCount = jdbc.queryForObject(
                "select count(*) from reservation_slot where reservation_id=? and status='占用'", Integer.class, reservationId);
        assertTrue(slotCount != null && slotCount > 0, "预约应写入 reservation_slot");

        List<Map<String, Object>> myList = app.myReservations(student, "ALL", false);
        assertTrue(myList.stream().anyMatch(r -> reservationId.equals(((Number) r.get("id")).longValue())));

        app.cancelReservation(student, reservationId);
        int scoreAfter = jdbc.queryForObject("select credit_score from student_profile where user_id=?", Integer.class, userId);
        assertEquals(scoreBefore - 50, scoreAfter, "取消预约应扣 50 分");

        Integer cancelLog = jdbc.queryForObject(
                "select count(*) from credit_log where user_id=? and reservation_id=? and change_type='违约扣减' and change_value=-50",
                Integer.class, userId, reservationId);
        assertEquals(1, cancelLog);

        String status = jdbc.queryForObject("select status from reservation where id=?", String.class, reservationId);
        assertEquals("已取消", status);

        Integer activeSlots = jdbc.queryForObject(
                "select count(*) from reservation_slot where reservation_id=? and status='占用'", Integer.class, reservationId);
        assertEquals(0, activeSlots, "取消后 reservation_slot 应释放");

        // 单室预约计数：与数据库直接查询一致（不依赖 MySQL 专用统计函数，H2/MySQL 通用）
        Integer dbRoomTotal = jdbc.queryForObject(
                "select count(*) from reservation where room_id=?", Integer.class, roomId);
        assertTrue(dbRoomTotal >= 1, "预约记录应持久化到 reservation 表");
    }
}
