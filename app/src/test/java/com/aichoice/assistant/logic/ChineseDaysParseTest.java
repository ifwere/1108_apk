package com.aichoice.assistant.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChineseDaysParseTest {

    private static final String REMOTE = "{"
            + "\"holidays\":{"
            + "\"2026-10-01\":\"National Day,国庆节,3\","
            + "\"2026-10-02\":\"National Day,国庆节,3\"},"
            + "\"workdays\":{"
            + "\"2026-10-10\":\"National Day,国庆节,3\"},"
            + "\"inLieuDays\":{"
            + "\"2026-10-05\":\"National Day,国庆节,3\"}"
            + "}";

    private static final String BUNDLED = "{"
            + "\"updated\":\"2026-11-07\","
            + "\"days\":["
            + "{\"date\":\"2025-01-01\",\"name\":\"元旦\"},"
            + "{\"date\":\"2026-10-01\",\"name\":\"旧名称\"},"
            + "{\"date\":\"2026-10-09\",\"name\":\"旧名称\"}"
            + "]}";

    @Test
    public void extractsChineseNameFromRemoteValue() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromChineseDaysJson(REMOTE);

        assertEquals("国庆节", calendar.holidayName(LocalDate.of(2026, 10, 1)));
        assertTrue(calendar.isHoliday(LocalDate.of(2026, 10, 2)));
    }

    @Test
    public void parsesMakeupWorkdaysButKeepsWeekendClassification() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromChineseDaysJson(REMOTE);

        LocalDate saturday = LocalDate.of(2026, 10, 10);
        assertTrue(calendar.isMakeupWorkday(saturday));
        assertEquals("国庆节", calendar.makeupWorkdayName(saturday));
        // The requested rule says weekends are always idle, even for make-up days.
        assertEquals(DayKind.WEEKEND, calendar.classify(saturday));
    }

    @Test
    public void ignoresMalformedDatesAndFallsBackToRawName() throws Exception {
        String json = "{\"holidays\":{"
                + "\"bad-date\":\"x,名称,1\","
                + "\"2026-01-01\":\"New Year\","
                + "\"2026-01-02\":\"New Year,元旦,1\""
                + "}}";

        HolidayCalendar calendar = HolidayCalendar.fromChineseDaysJson(json);

        assertEquals("New Year", calendar.holidayName(LocalDate.of(2026, 1, 1)));
        assertEquals("元旦", calendar.holidayName(LocalDate.of(2026, 1, 2)));
    }

    @Test
    public void mergeReplacesOnlyCoveredYears() throws Exception {
        HolidayCalendar bundled = HolidayCalendar.fromJson(BUNDLED);
        HolidayCalendar remote = HolidayCalendar.fromChineseDaysJson(REMOTE);

        HolidayCalendar merged = remote.mergeOver(bundled);

        // 2025 is not covered by the remote snapshot: bundled data survives.
        assertEquals("元旦", merged.holidayName(LocalDate.of(2025, 1, 1)));
        // 2026 is covered: the remote name wins and stale bundled dates are gone.
        assertEquals("国庆节", merged.holidayName(LocalDate.of(2026, 10, 1)));
        assertFalse(merged.isHoliday(LocalDate.of(2026, 10, 9)));
    }

    @Test
    public void customHolidaysOverrideRemoteData() throws Exception {
        Map<LocalDate, String> custom = new HashMap<>();
        custom.put(LocalDate.of(2026, 10, 1), "公司假期");

        HolidayCalendar merged = HolidayCalendar.fromChineseDaysJson(REMOTE)
                .withAdditionalHolidays(custom);

        assertEquals("公司假期", merged.holidayName(LocalDate.of(2026, 10, 1)));
    }

    @Test
    public void customRestDayOverridesMakeupWorkday() throws Exception {
        Map<LocalDate, String> custom = new HashMap<>();
        custom.put(LocalDate.of(2026, 10, 10), "公司假期");

        HolidayCalendar merged = HolidayCalendar.fromChineseDaysJson(REMOTE)
                .withAdditionalHolidays(custom);

        assertFalse(merged.isMakeupWorkday(LocalDate.of(2026, 10, 10)));
        assertEquals(DayKind.HOLIDAY, merged.classify(LocalDate.of(2026, 10, 10)));
    }

    @Test
    public void maxYearIncludesMakeupWorkdays() throws Exception {
        String json = "{\"holidays\":{\"2026-10-01\":\"National Day,国庆节,3\"},"
                + "\"workdays\":{\"2027-01-10\":\"New Year,元旦,1\"}}";

        HolidayCalendar calendar = HolidayCalendar.fromChineseDaysJson(json);

        assertEquals(2027, calendar.maxYear());
        assertTrue(calendar.hasYear(2027));
        assertFalse(calendar.hasYear(2028));
    }

    @Test
    public void emptyRemoteMergeReturnsFallback() throws Exception {
        HolidayCalendar remote = HolidayCalendar.fromChineseDaysJson("{\"holidays\":{}}");
        HolidayCalendar bundled = HolidayCalendar.fromJson(BUNDLED);

        assertTrue(remote.mergeOver(bundled) == bundled);
        assertTrue(remote.withAdditionalHolidays(Collections.emptyMap()) == remote);
    }
}
