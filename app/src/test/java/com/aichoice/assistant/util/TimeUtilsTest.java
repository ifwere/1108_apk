package com.aichoice.assistant.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtilsTest {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Test
    public void formatsMinutes() {
        assertEquals("00:00", TimeUtils.formatMinute(0));
        assertEquals("09:05", TimeUtils.formatMinute(9 * 60 + 5));
        assertEquals("12:00", TimeUtils.formatMinute(12 * 60));
        assertEquals("23:59", TimeUtils.formatMinute(23 * 60 + 59));
    }

    @Test
    public void formatsSameDayRange() {
        assertEquals("09:00–12:00", TimeUtils.formatRange(9 * 60, 12 * 60));
        assertEquals("14:00–18:00", TimeUtils.formatRange(14 * 60, 18 * 60));
    }

    @Test
    public void formatsOvernightAndMidnightRanges() {
        assertEquals("22:30–次日 06:00", TimeUtils.formatRange(22 * 60 + 30, 6 * 60));
        assertEquals("21:00–24:00", TimeUtils.formatRange(21 * 60, 0));
        assertEquals("全天", TimeUtils.formatRange(0, 0));
        assertEquals("全天", TimeUtils.formatRange(9 * 60, 9 * 60));
    }

    @Test
    public void formatsEndTime() {
        assertEquals("24:00", TimeUtils.formatEnd(0));
        assertEquals("18:00", TimeUtils.formatEnd(18 * 60));
    }

    @Test
    public void formatsWeekdaySelections() {
        assertEquals("每天", TimeUtils.formatDays(
                new boolean[]{true, true, true, true, true, true, true}));
        assertEquals("周一至周五", TimeUtils.formatDays(
                new boolean[]{true, true, true, true, true, false, false}));
        assertEquals("周末", TimeUtils.formatDays(
                new boolean[]{false, false, false, false, false, true, true}));
        assertEquals("周一、周三", TimeUtils.formatDays(
                new boolean[]{true, false, true, false, false, false, false}));
        assertEquals("未选择", TimeUtils.formatDays(
                new boolean[]{false, false, false, false, false, false, false}));
    }

    @Test
    public void formatsBeijingDateAndClock() {
        java.time.ZonedDateTime time = LocalDateTime.of(2026, 11, 7, 23, 41)
                .atZone(BEIJING);
        assertEquals("11月7日 周六", TimeUtils.formatDate(time));
        assertEquals("23:41", TimeUtils.formatClock(time));
        assertEquals("周一", TimeUtils.weekdayLabel(0));
        assertEquals("周日", TimeUtils.weekdayLabel(6));
    }

    @Test
    public void formatsFullDateForCustomHolidays() {
        assertEquals("2027年10月8日 周五",
                TimeUtils.formatFullDate(LocalDate.of(2027, 10, 8)));
        assertEquals("2026年11月7日 周六",
                TimeUtils.formatFullDate(LocalDate.of(2026, 11, 7)));
    }
}
