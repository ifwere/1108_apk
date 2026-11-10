package com.aichoice.assistant.util;

import android.content.Context;

import com.aichoice.assistant.R;
import com.aichoice.assistant.logic.DayKind;
import com.aichoice.assistant.logic.HolidayCalendar;

import java.time.LocalDate;

/**
 * Builds the weekday / holiday label shown on the recommendation cards.
 */
public final class DayLabels {

    private DayLabels() {
    }

    public static String of(Context context, HolidayCalendar calendar, LocalDate date) {
        DayKind kind = calendar.classify(date);
        if (kind == DayKind.HOLIDAY) {
            String name = calendar.holidayName(date);
            return name == null ? context.getString(R.string.day_holiday) : name;
        }
        if (kind == DayKind.WEEKEND) {
            return context.getString(calendar.isMakeupWorkday(date)
                    ? R.string.day_makeup_weekend
                    : R.string.day_weekend);
        }
        return context.getString(R.string.day_workday);
    }
}
