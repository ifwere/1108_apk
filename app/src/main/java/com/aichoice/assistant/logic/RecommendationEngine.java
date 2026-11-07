package com.aichoice.assistant.logic;

import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.Target;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Decides which AI tool to recommend at a given Beijing-time instant.
 *
 * <p>Rules are evaluated in list order; the first enabled matching rule wins.
 * A rule matches when the weekday is selected, the time falls inside
 * {@code [start, end)} and, if requested, the date is not a statutory holiday.
 * If no rule matches, DeepSeek is used as the idle-time fallback.</p>
 */
public final class RecommendationEngine {

    public static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final HolidayCalendar calendar;

    public RecommendationEngine(HolidayCalendar calendar) {
        this.calendar = calendar == null ? HolidayCalendar.empty() : calendar;
    }

    public Recommendation evaluate(ZonedDateTime now, List<Rule> rules) {
        ZonedDateTime beijingTime = now.withZoneSameInstant(BEIJING_ZONE);
        LocalDate date = beijingTime.toLocalDate();
        int minuteOfDay = beijingTime.getHour() * 60 + beijingTime.getMinute();
        int dayIndex = beijingTime.getDayOfWeek().getValue() - 1;
        boolean holiday = calendar.isHoliday(date);

        List<Rule> safeRules = rules == null ? Collections.emptyList() : rules;
        for (Rule rule : safeRules) {
            if (rule == null || !rule.isEnabled()) {
                continue;
            }
            if (!rule.isDaySelected(dayIndex)) {
                continue;
            }
            if (rule.isSkipHolidays() && holiday) {
                continue;
            }
            if (!matchesTime(rule, minuteOfDay)) {
                continue;
            }
            return new Recommendation(rule.getTarget(), rule.getId(), rule.getName(), false);
        }

        // No rule matched: idle time is always DeepSeek.
        return new Recommendation(Target.DEEPSEEK, null, null, true);
    }

    static boolean matchesTime(Rule rule, int minuteOfDay) {
        int start = rule.getStartMinute();
        int end = rule.getEndMinute();
        if (start == end) {
            // An all-day rule.
            return true;
        }
        if (start < end) {
            return minuteOfDay >= start && minuteOfDay < end;
        }
        // Cross-midnight range, e.g. 22:00 - 06:00.
        return minuteOfDay >= start || minuteOfDay < end;
    }
}
