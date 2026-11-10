package com.aichoice.assistant.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.Target;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Covers the lookahead used by the "下一次 DeepSeek 低价时间" subtitle.
 */
public class NextLowPriceTest {

    private static final ZoneId BEIJING = RecommendationEngine.BEIJING_ZONE;
    private static final String HOLIDAY_JSON = "{"
            + "\"updated\":\"2026-11-07\","
            + "\"days\":["
            + "{\"date\":\"2026-10-01\",\"name\":\"国庆节\"}"
            + "]}";

    private RecommendationEngine engine;
    private List<Rule> defaults;

    @Before
    public void setUp() throws Exception {
        engine = new RecommendationEngine(HolidayCalendar.fromJson(HOLIDAY_JSON));
        defaults = new ArrayList<>();
        defaults.add(rule("工作日高峰 · 上午", Target.WORKBUDDY, weekdays(),
                9 * 60, 12 * 60, true));
        defaults.add(rule("工作日高峰 · 下午", Target.WORKBUDDY, weekdays(),
                14 * 60, 18 * 60, true));
        defaults.add(rule("其余时间", Target.DEEPSEEK, allDays(), 0, 0, false));
    }

    @Test
    public void morningPeakLooksAheadToNoon() {
        // Monday 2026-11-09 10:00 -> DeepSeek starts at 12:00.
        assertEquals(at(2026, 11, 9, 12, 0),
                engine.nextDeepSeekStart(at(2026, 11, 9, 10, 0), defaults));
    }

    @Test
    public void afternoonPeakLooksAheadToEvening() {
        assertEquals(at(2026, 11, 9, 18, 0),
                engine.nextDeepSeekStart(at(2026, 11, 9, 15, 0), defaults));
    }

    @Test
    public void idleWindowEndsWhenTheNextPeakStarts() {
        // Lunch break 12:30 -> afternoon peak starts at 14:00.
        assertEquals(at(2026, 11, 9, 14, 0),
                engine.nextDeepSeekEnd(at(2026, 11, 9, 12, 30), defaults));
    }

    @Test
    public void idleEveningEndsOnTheNextWorkdayMorning() {
        assertEquals(at(2026, 11, 10, 9, 0),
                engine.nextDeepSeekEnd(at(2026, 11, 9, 20, 0), defaults));
    }

    @Test
    public void weekendIdleEndsOnMondayMorning() {
        // Saturday 2026-11-07 10:00 -> Monday 2026-11-09 09:00.
        assertEquals(at(2026, 11, 9, 9, 0),
                engine.nextDeepSeekEnd(at(2026, 11, 7, 10, 0), defaults));
    }

    @Test
    public void statutoryHolidayIsIdleUntilTheNextWorkdayPeak() {
        assertEquals(at(2026, 10, 2, 9, 0),
                engine.nextDeepSeekEnd(at(2026, 10, 1, 10, 0), defaults));
    }

    @Test
    public void searchStartsAtTheNextMinuteBoundary() {
        // 11:59:30 -> the transition is still reported at 12:00.
        ZonedDateTime now = LocalDateTime.of(2026, 11, 9, 11, 59, 30)
                .atZone(BEIJING);
        assertEquals(at(2026, 11, 9, 12, 0), engine.nextDeepSeekStart(now, defaults));
    }

    @Test
    public void alwaysIdleHasNoEnd() {
        assertNull(engine.nextDeepSeekEnd(at(2026, 11, 9, 10, 0),
                Collections.emptyList()));
    }

    private static ZonedDateTime at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(BEIJING);
    }

    private static Rule rule(String name, Target target, boolean[] days,
                             int startMinute, int endMinute, boolean skipHolidays) {
        Rule rule = new Rule();
        rule.setName(name);
        rule.setTarget(target);
        rule.setDays(days);
        rule.setStartMinute(startMinute);
        rule.setEndMinute(endMinute);
        rule.setSkipHolidays(skipHolidays);
        return rule;
    }

    private static boolean[] weekdays() {
        return new boolean[]{true, true, true, true, true, false, false};
    }

    private static boolean[] allDays() {
        return new boolean[]{true, true, true, true, true, true, true};
    }
}
