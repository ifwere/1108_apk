package com.aichoice.assistant.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.Target;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendationEngineTest {

    private static final ZoneId BEIJING = RecommendationEngine.BEIJING_ZONE;
    private static final String HOLIDAY_JSON = "{"
            + "\"updated\":\"2026-11-07\","
            + "\"days\":["
            + "{\"date\":\"2026-10-01\",\"name\":\"国庆节\"},"
            + "{\"date\":\"2026-02-17\",\"name\":\"春节\"},"
            + "{\"date\":\"2025-10-01\",\"name\":\"国庆节\"}"
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
    public void peakTimeBoundariesUseWorkBuddy() {
        // Monday 2026-11-09.
        assertEquals(Target.DEEPSEEK, targetAt(2026, 11, 9, 8, 59));
        assertEquals(Target.WORKBUDDY, targetAt(2026, 11, 9, 9, 0));
        assertEquals(Target.WORKBUDDY, targetAt(2026, 11, 9, 11, 59));
        assertEquals(Target.DEEPSEEK, targetAt(2026, 11, 9, 12, 0));
        assertEquals(Target.DEEPSEEK, targetAt(2026, 11, 9, 13, 59));
        assertEquals(Target.WORKBUDDY, targetAt(2026, 11, 9, 14, 0));
        assertEquals(Target.WORKBUDDY, targetAt(2026, 11, 9, 17, 59));
        assertEquals(Target.DEEPSEEK, targetAt(2026, 11, 9, 18, 0));
    }

    @Test
    public void weekendIsIdleTime() {
        // Saturday 2026-11-07 at 10:00, which would be peak on a weekday.
        Recommendation recommendation = evaluate(2026, 11, 7, 10, 0);
        assertEquals(Target.DEEPSEEK, recommendation.getTarget());
        assertFalse(recommendation.isFallback());
        assertEquals("其余时间", recommendation.getRuleName());
    }

    @Test
    public void statutoryHolidayIsIdleTime() {
        // Thursday 2026-10-01, National Day.
        Recommendation recommendation = evaluate(2026, 10, 1, 10, 0);
        assertEquals(Target.DEEPSEEK, recommendation.getTarget());
        assertEquals("其余时间", recommendation.getRuleName());
    }

    @Test
    public void customHolidayMakesAWeekdayIdle() throws Exception {
        HolidayCalendar calendar = HolidayCalendar.fromJson(HOLIDAY_JSON)
                .withAdditionalHolidays(Collections.singletonMap(
                        LocalDate.of(2026, 11, 10), "公司假期"));
        RecommendationEngine customEngine = new RecommendationEngine(calendar);

        // Tuesday 2026-11-10 at 10:00 would be peak without the custom holiday.
        Recommendation recommendation = customEngine.evaluate(at(2026, 11, 10, 10, 0), defaults);
        assertEquals(Target.DEEPSEEK, recommendation.getTarget());
        assertEquals("其余时间", recommendation.getRuleName());
    }

    @Test
    public void holidayStillMatchesWhenRuleDoesNotSkipHolidays() {
        List<Rule> rules = new ArrayList<>();
        Rule holidayRule = rule("节假日值班", Target.WORKBUDDY, weekdays(),
                9 * 60, 12 * 60, false);
        rules.add(holidayRule);
        rules.add(rule("其余时间", Target.DEEPSEEK, allDays(), 0, 0, false));

        Recommendation recommendation = engine.evaluate(at(2026, 10, 1, 10, 0), rules);
        assertEquals(Target.WORKBUDDY, recommendation.getTarget());
        assertEquals("节假日值班", recommendation.getRuleName());
    }

    @Test
    public void overnightRangeIsSupported() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("夜间", Target.WORKBUDDY, allDays(), 22 * 60, 6 * 60, false));
        rules.add(rule("其余时间", Target.DEEPSEEK, allDays(), 0, 0, false));

        assertEquals(Target.WORKBUDDY, engine.evaluate(at(2026, 11, 9, 23, 0), rules).getTarget());
        assertEquals(Target.WORKBUDDY, engine.evaluate(at(2026, 11, 9, 5, 59), rules).getTarget());
        assertEquals(Target.DEEPSEEK, engine.evaluate(at(2026, 11, 9, 6, 0), rules).getTarget());
        assertEquals(Target.DEEPSEEK, engine.evaluate(at(2026, 11, 9, 21, 59), rules).getTarget());
    }

    @Test
    public void disabledRulesAreIgnored() {
        List<Rule> rules = new ArrayList<>();
        Rule disabled = rule("停用", Target.WORKBUDDY, weekdays(), 9 * 60, 12 * 60, true);
        disabled.setEnabled(false);
        rules.add(disabled);
        rules.add(rule("其余时间", Target.DEEPSEEK, allDays(), 0, 0, false));

        assertEquals(Target.DEEPSEEK, engine.evaluate(at(2026, 11, 9, 10, 0), rules).getTarget());
    }

    @Test
    public void firstMatchingRuleWins() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("先命中", Target.DEEPSEEK, weekdays(), 9 * 60, 12 * 60, true));
        rules.add(rule("后命中", Target.WORKBUDDY, weekdays(), 9 * 60, 12 * 60, true));

        Recommendation recommendation = engine.evaluate(at(2026, 11, 9, 10, 0), rules);
        assertEquals(Target.DEEPSEEK, recommendation.getTarget());
        assertEquals("先命中", recommendation.getRuleName());
    }

    @Test
    public void noRulesFallsBackToDeepSeek() {
        Recommendation recommendation = engine.evaluate(at(2026, 11, 9, 10, 0),
                Collections.emptyList());
        assertEquals(Target.DEEPSEEK, recommendation.getTarget());
        assertTrue(recommendation.isFallback());
    }

    @Test
    public void ruleOnlyAppliesOnSelectedWeekdays() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("仅周一", Target.WORKBUDDY, mondayOnly(), 9 * 60, 12 * 60, true));
        rules.add(rule("其余时间", Target.DEEPSEEK, allDays(), 0, 0, false));

        assertEquals(Target.WORKBUDDY, engine.evaluate(at(2026, 11, 9, 10, 0), rules).getTarget());
        assertEquals(Target.DEEPSEEK, engine.evaluate(at(2026, 11, 10, 10, 0), rules).getTarget());
    }

    @Test
    public void allDayRuleMatchesAnyTime() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("全天", Target.WORKBUDDY, allDays(), 0, 0, false));

        assertEquals(Target.WORKBUDDY, engine.evaluate(at(2026, 11, 9, 3, 15), rules).getTarget());
        assertEquals(Target.WORKBUDDY, engine.evaluate(at(2026, 11, 9, 23, 59), rules).getTarget());
    }

    @Test
    public void evaluationUsesBeijingTimeRegardlessOfInputZone() {
        // 01:30 UTC == 09:30 Beijing on Monday 2026-11-09.
        ZonedDateTime utc = ZonedDateTime.of(2026, 11, 9, 1, 30, 0, 0, ZoneOffset.UTC);
        Recommendation recommendation = engine.evaluate(utc, defaults);
        assertEquals(Target.WORKBUDDY, recommendation.getTarget());
    }

    @Test
    public void classifiesHolidayWeekendAndWorkday() {
        assertEquals(DayKind.HOLIDAY, engineHolidayCalendar().classify(
                java.time.LocalDate.of(2026, 10, 1)));
        assertEquals(DayKind.WEEKEND, engineHolidayCalendar().classify(
                java.time.LocalDate.of(2026, 11, 7)));
        assertEquals(DayKind.WORKDAY, engineHolidayCalendar().classify(
                java.time.LocalDate.of(2026, 11, 9)));
    }

    private HolidayCalendar engineHolidayCalendar() {
        try {
            return HolidayCalendar.fromJson(HOLIDAY_JSON);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Recommendation evaluate(int year, int month, int day, int hour, int minute) {
        return engine.evaluate(at(year, month, day, hour, minute), defaults);
    }

    private Target targetAt(int year, int month, int day, int hour, int minute) {
        return evaluate(year, month, day, hour, minute).getTarget();
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

    private static boolean[] mondayOnly() {
        return new boolean[]{true, false, false, false, false, false, false};
    }
}
