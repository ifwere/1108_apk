package com.aichoice.assistant.ui;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aichoice.assistant.logic.HolidayCalendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** RecyclerView adapter that renders the future months as calendar grids. */
public final class MonthCalendarAdapter
        extends RecyclerView.Adapter<MonthCalendarAdapter.MonthHolder> {

    private final List<YearMonth> months = new ArrayList<>();
    private HolidayCalendar calendar = HolidayCalendar.empty();
    private Set<LocalDate> customDates = Collections.emptySet();
    private LocalDate today = LocalDate.now();

    public void submit(List<YearMonth> newMonths,
                       HolidayCalendar newCalendar,
                       Set<LocalDate> newCustomDates,
                       LocalDate newToday) {
        months.clear();
        if (newMonths != null) {
            months.addAll(newMonths);
        }
        calendar = newCalendar == null ? HolidayCalendar.empty() : newCalendar;
        customDates = newCustomDates == null
                ? Collections.emptySet() : newCustomDates;
        today = newToday == null ? LocalDate.now() : newToday;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MonthCalendarView view = new MonthCalendarView(parent.getContext());
        view.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return new MonthHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthHolder holder, int position) {
        holder.view.bind(months.get(position), calendar, customDates, today);
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    static final class MonthHolder extends RecyclerView.ViewHolder {
        final MonthCalendarView view;

        MonthHolder(MonthCalendarView view) {
            super(view);
            this.view = view;
        }
    }
}
