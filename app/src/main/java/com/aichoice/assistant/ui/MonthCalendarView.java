package com.aichoice.assistant.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.aichoice.assistant.R;
import com.aichoice.assistant.databinding.ItemCalendarDayBinding;
import com.aichoice.assistant.databinding.ItemMonthCalendarBinding;
import com.aichoice.assistant.logic.HolidayCalendar;
import com.aichoice.assistant.util.TimeUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * Renders one month as a Monday-first grid. Holidays, make-up workdays and
 * user-defined dates get distinct markers.
 */
public final class MonthCalendarView extends LinearLayout {

    private static final int COLUMNS = 7;

    private final ItemMonthCalendarBinding binding;

    public MonthCalendarView(Context context) {
        this(context, null);
    }

    public MonthCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        binding = ItemMonthCalendarBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public void bind(YearMonth month,
                     HolidayCalendar calendar,
                     Set<LocalDate> customDates,
                     LocalDate today) {
        binding.tvMonthTitle.setText(getContext().getString(
                R.string.calendar_month_title, month.getYear(), month.getMonthValue()));

        int holidayCount = 0;
        int makeupCount = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            if (calendar.isHoliday(date)) {
                holidayCount++;
            }
            if (calendar.isMakeupWorkday(date)) {
                makeupCount++;
            }
        }
        binding.tvMonthSummary.setText(getContext().getString(
                R.string.calendar_month_summary, holidayCount, makeupCount));

        buildGrid(month, calendar, customDates, today);
    }

    private void buildGrid(YearMonth month,
                           HolidayCalendar calendar,
                           Set<LocalDate> customDates,
                           LocalDate today) {
        GridLayout grid = binding.gridCalendar;
        grid.removeAllViews();

        for (int i = 0; i < COLUMNS; i++) {
            grid.addView(weekdayHeader(i),
                    cellParams(i, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        int index = COLUMNS;
        int firstOffset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < firstOffset; i++) {
            grid.addView(spacer(), cellParams(index++, dp(54)));
        }

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            grid.addView(dayCell(date, calendar, customDates, today),
                    cellParams(index++, dp(54)));
        }

        // Pad the last row so the grid keeps a stable shape.
        while (index % COLUMNS != 0) {
            grid.addView(spacer(), cellParams(index++, dp(54)));
        }
    }

    private TextView weekdayHeader(int column) {
        TextView view = new TextView(getContext());
        view.setText(TimeUtils.DAY_LABELS[column].substring(1));
        view.setGravity(android.view.Gravity.CENTER);
        view.setTextColor(ContextCompat.getColor(getContext(), R.color.text_tertiary));
        view.setTextSize(11);
        return view;
    }

    private View spacer() {
        return new View(getContext());
    }

    private View dayCell(LocalDate date,
                         HolidayCalendar calendar,
                         Set<LocalDate> customDates,
                         LocalDate today) {
        ItemCalendarDayBinding cell = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(getContext()), binding.gridCalendar, false);

        boolean custom = customDates != null && customDates.contains(date);
        boolean holiday = calendar.isHoliday(date);
        boolean makeup = calendar.isMakeupWorkday(date);
        boolean weekend = date.getDayOfWeek().getValue() >= 6;

        cell.tvDay.setText(String.valueOf(date.getDayOfMonth()));

        int textColor;
        if (custom) {
            cell.getRoot().setBackgroundResource(R.drawable.bg_calendar_cell_custom);
            textColor = ContextCompat.getColor(getContext(), R.color.primary);
            cell.tvLabel.setText(shortName(calendar.holidayName(date)));
            cell.tvLabel.setTextColor(textColor);
        } else if (holiday) {
            cell.getRoot().setBackgroundResource(R.drawable.bg_calendar_cell_holiday);
            textColor = ContextCompat.getColor(getContext(), R.color.calendar_holiday_text);
            cell.tvLabel.setText(shortName(calendar.holidayName(date)));
            cell.tvLabel.setTextColor(textColor);
        } else {
            textColor = ContextCompat.getColor(getContext(),
                    weekend ? R.color.text_tertiary : R.color.text_primary);
            cell.tvLabel.setVisibility(View.GONE);
        }
        cell.tvDay.setTextColor(textColor);

        cell.tvBadge.setVisibility(makeup ? View.VISIBLE : View.GONE);
        cell.viewTodayRing.setVisibility(date.equals(today) ? View.VISIBLE : View.GONE);

        cell.getRoot().setContentDescription(TimeUtils.formatFullDate(date)
                + (custom ? " 自定义" : holiday ? " 休息" : "")
                + (makeup ? " 补班" : ""));
        return cell.getRoot();
    }

    private static String shortName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String safe = name.trim();
        return safe.length() <= 4 ? safe : safe.substring(0, 4);
    }

    private GridLayout.LayoutParams cellParams(int index, int height) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        params.columnSpec = GridLayout.spec(index % COLUMNS, 1f);
        params.rowSpec = GridLayout.spec(index / COLUMNS);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
