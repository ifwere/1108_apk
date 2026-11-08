package com.aichoice.assistant.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CalendarRangeTest {

    @Test
    public void startsAtCurrentMonthAndEndsAtDecemberOfLastDataYear() {
        List<YearMonth> months = CalendarMonths.range(YearMonth.of(2026, 11), 2027);

        assertEquals(14, months.size());
        assertEquals(YearMonth.of(2026, 11), months.get(0));
        assertEquals(YearMonth.of(2027, 12), months.get(months.size() - 1));
    }

    @Test
    public void endYearIsNeverBeforeCurrentYear() {
        List<YearMonth> months = CalendarMonths.range(YearMonth.of(2026, 11), 2025);

        assertEquals(2, months.size());
        assertEquals(YearMonth.of(2026, 12), months.get(1));
    }

    @Test
    public void noDataShowsOnlyCurrentMonth() {
        List<YearMonth> months = CalendarMonths.range(YearMonth.of(2026, 11), 0);

        assertEquals(1, months.size());
        assertEquals(YearMonth.of(2026, 11), months.get(0));
    }

    @Test
    public void acceptsLocalDateAndHandlesNull() {
        List<YearMonth> months = CalendarMonths.range(LocalDate.of(2026, 11, 8), 2026);

        assertEquals(2, months.size());
        assertTrue(CalendarMonths.range((LocalDate) null, 2026).isEmpty());
    }
}
