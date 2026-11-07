package com.aichoice.assistant.util;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Formatting helpers shared by the UI and unit tests.
 */
public final class TimeUtils {

    public static final String[] DAY_LABELS = {
            "周一", "周二", "周三", "周四", "周五", "周六", "周日"
    };

    private TimeUtils() {
    }

    /** Formats a minute-of-day as HH:mm. */
    public static String formatMinute(int minuteOfDay) {
        int safe = Math.max(0, Math.min(24 * 60 - 1, minuteOfDay));
        return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60);
    }

    /** Formats a range such as "09:00–12:00", "22:00–次日 06:00" or "全天". */
    public static String formatRange(int startMinute, int endMinute) {
        if (startMinute == endMinute) {
            return "全天";
        }
        String start = formatMinute(startMinute);
        if (endMinute == 0) {
            return start + "–24:00";
        }
        if (startMinute < endMinute) {
            return start + "–" + formatMinute(endMinute);
        }
        return start + "–次日 " + formatMinute(endMinute);
    }

    /** Label used on the "end time" button. Midnight is shown as 24:00. */
    public static String formatEnd(int endMinute) {
        return endMinute == 0 ? "24:00" : formatMinute(endMinute);
    }

    /** Formats the selected weekdays, e.g. "周一至周五", "周末", "每天". */
    public static String formatDays(boolean[] days) {
        if (days == null) {
            return "未选择";
        }
        int selected = 0;
        for (boolean day : days) {
            if (day) {
                selected++;
            }
        }
        if (selected == 0) {
            return "未选择";
        }
        if (selected == 7) {
            return "每天";
        }
        if (selected == 5 && days[0] && days[1] && days[2] && days[3] && days[4]) {
            return "周一至周五";
        }
        if (selected == 2 && days[5] && days[6]) {
            return "周末";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(days.length, DAY_LABELS.length); i++) {
            if (days[i]) {
                if (builder.length() > 0) {
                    builder.append('、');
                }
                builder.append(DAY_LABELS[i]);
            }
        }
        return builder.toString();
    }

    /** "11月7日 周六" in Beijing time. */
    public static String formatDate(ZonedDateTime beijingTime) {
        return beijingTime.getMonthValue() + "月" + beijingTime.getDayOfMonth() + "日 "
                + weekdayLabel(beijingTime.getDayOfWeek().getValue() - 1);
    }

    /** Full date label used by the custom-holiday list, e.g. "2027年10月8日 周五". */
    public static String formatFullDate(LocalDate date) {
        return date.getYear() + "年" + date.getMonthValue() + "月" + date.getDayOfMonth() + "日 "
                + weekdayLabel(date.getDayOfWeek().getValue() - 1);
    }

    public static String formatClock(ZonedDateTime beijingTime) {
        return String.format(Locale.US, "%02d:%02d",
                beijingTime.getHour(), beijingTime.getMinute());
    }

    public static String weekdayLabel(int zeroBasedDayIndex) {
        int safe = Math.max(0, Math.min(DAY_LABELS.length - 1, zeroBasedDayIndex));
        return DAY_LABELS[safe];
    }
}
