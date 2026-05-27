package com.scau.campusstudyroomreservationmanagementsystem.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 纯单元测试：不依赖数据库，验证 SQL 片段拼接无粘连。 */
class SqlFragmentsGlueRegressionTest {

    @Test
    void adminDashboardSqlFragmentsShouldNotGlue() {
        String weeklyWhere = SqlFragments.join("where",
                "r.reserve_date between date_sub(current_date(), interval 6 day) and current_date()",
                "and", "sr.manager_id=1");
        assertFalse(SqlFragments.hasGlueBug(weeklyWhere));

        String peakSql = SqlFragments.join("""
                select hour(r.start_time) AS peakHour, count(*) AS cnt
                from reservation r
                join study_room sr on sr.id=r.room_id""",
                SqlFragments.join("where", "r.reserve_date=current_date()", "and", "sr.manager_id=1"),
                "group by hour(r.start_time)", "order by hour(r.start_time)");
        assertFalse(SqlFragments.hasGlueBug(peakSql));
        assertTrue(peakSql.contains("where r.reserve_date"));
        assertTrue(peakSql.contains("manager_id=1 group by"));
    }
}
