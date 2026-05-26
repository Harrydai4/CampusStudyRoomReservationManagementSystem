package com.scau.campusstudyroomreservationmanagementsystem.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlFragmentsTest {

    @Test
    void joinShouldInsertSpacesBetweenKeywords() {
        String where = SqlFragments.join("where", "r.reserve_date=current_date()");
        assertEquals("where r.reserve_date=current_date()", where);
        assertFalse(SqlFragments.hasGlueBug(where));

        String peak = SqlFragments.join("where", "r.reserve_date=current_date()", "and", "sr.manager_id=1",
                "group by hour(r.start_time)", "order by hour");
        assertFalse(SqlFragments.hasGlueBug(peak));
        assertTrue(peak.contains("manager_id=1 group by"));
    }

    @Test
    void hasGlueBugShouldDetectKnownMistakes() {
        assertTrue(SqlFragments.hasGlueBug("where r.x=1 and sr.manager_id=1group by hour"));
        assertTrue(SqlFragments.hasGlueBug("on r.room_id=sr.id andr.reserve_date=current_date()"));
        assertTrue(SqlFragments.hasGlueBug("where r.x wherer.y=1"));
    }
}
