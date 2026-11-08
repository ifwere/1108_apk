package com.aichoice.assistant.logic;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the months shown by the calendar page: from the current month up to
 * December of the last year that has holiday data. Past months are never
 * shown.
 */
public final class CalendarMonths {

    private CalendarMonths() {
    }

    public static List<YearMonth> range(LocalDate today, int dataMaxYear) {
        if (today == null) {
            return new ArrayList<>();
        }
        return range(YearMonth.from(today), dataMaxYear);
    }

    public static List<YearMonth> range(YearMonth start, int dataMaxYear) {
        List<YearMonth> months = new ArrayList<>();
        if (start == null) {
            return months;
        }
        YearMonth end;
        if (dataMaxYear <= 0) {
            // No data at all: only the current month is meaningful.
            end = start;
        } else {
            int endYear = Math.max(start.getYear(), dataMaxYear);
            end = YearMonth.of(endYear, 12);
        }
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }
}
