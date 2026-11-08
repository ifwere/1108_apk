package com.aichoice.assistant.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HolidayCalendarTest {

    private static final String JSON = "{"
            + "\"updated\":\"2026-11-07\","
            + "\"days\":["
            + "{\"date\":\"2025-01-01\",\"name\":\"元旦\"},"
            + "{\"date\":\"2026-02-17\",\"name\":\"春节\"},"
            + "{\"date\":\"2026-10-07\",\"name\":\"国庆节\"},"
            + "{\"date\":\"not-a-date\",\"name\":\"bad\"}"
            + "]}";

    @Test
    public void parsesHolidayNamesAndMetadata() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(JSON);

        assertTrue(calendar.isHoliday(LocalDate.of(2026, 2, 17)));
        assertEquals("春节", calendar.holidayName(LocalDate.of(2026, 2, 17)));
        assertEquals("2026-11-07", calendar.updated());
        assertEquals(2025, calendar.firstYear());
        assertEquals(2026, calendar.lastYear());
        assertEquals("2025-2026", calendar.yearRange());
    }

    @Test
    public void ignoresMalformedEntriesAndUnknownDates() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(JSON);

        assertFalse(calendar.isHoliday(LocalDate.of(2026, 11, 7)));
        assertNull(calendar.holidayName(LocalDate.of(2026, 11, 7)));
        assertFalse(calendar.isHoliday(LocalDate.of(2026, 10, 8)));
    }

    @Test
    public void classifiesDays() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(JSON);

        // 2026-02-17 is both a Tuesday and a holiday: holiday wins.
        assertEquals(DayKind.HOLIDAY, calendar.classify(LocalDate.of(2026, 2, 17)));
        assertEquals(DayKind.WEEKEND, calendar.classify(LocalDate.of(2026, 11, 7)));
        assertEquals(DayKind.WORKDAY, calendar.classify(LocalDate.of(2026, 11, 9)));
    }

    @Test
    public void emptyCalendarHasNoYears() {
        HolidayCalendar calendar = HolidayCalendar.empty();

        assertFalse(calendar.isHoliday(LocalDate.of(2026, 10, 1)));
        assertEquals("-", calendar.yearRange());
        assertEquals("", calendar.updated());
    }

    @Test
    public void customHolidaysMergeOverBuiltInData() throws Exception {
        HolidayCalendar base = HolidayCalendar.fromJson(JSON);
        Map<LocalDate, String> custom = new HashMap<>();
        custom.put(LocalDate.of(2026, 10, 7), "公司假期");
        custom.put(LocalDate.of(2027, 10, 8), "调休");

        HolidayCalendar merged = base.withAdditionalHolidays(custom);

        assertEquals("公司假期", merged.holidayName(LocalDate.of(2026, 10, 7)));
        assertEquals("调休", merged.holidayName(LocalDate.of(2027, 10, 8)));
        assertTrue(merged.isHoliday(LocalDate.of(2027, 10, 8)));
        // Built-in entries not overridden are preserved.
        assertEquals("春节", merged.holidayName(LocalDate.of(2026, 2, 17)));
        assertEquals("2025-2027", merged.yearRange());
        // The original calendar is not mutated.
        assertEquals("国庆节", base.holidayName(LocalDate.of(2026, 10, 7)));
        assertFalse(base.isHoliday(LocalDate.of(2027, 10, 8)));
    }

    @Test
    public void blankCustomNamesFallBackToDefault() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(JSON)
                .withAdditionalHolidays(Collections.singletonMap(
                        LocalDate.of(2027, 1, 4), "  "));

        assertEquals("自定义节假日", calendar.holidayName(LocalDate.of(2027, 1, 4)));
        assertEquals(DayKind.HOLIDAY, calendar.classify(LocalDate.of(2027, 1, 4)));
    }

    @Test
    public void emptyCustomMapReturnsSameCalendar() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(JSON);

        assertTrue(calendar == calendar.withAdditionalHolidays(Collections.emptyMap()));
    }
}
