package com.aichoice.assistant;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aichoice.assistant.data.CustomHolidayRepository;
import com.aichoice.assistant.data.HolidayCacheRepository;
import com.aichoice.assistant.data.HolidayDataProvider;
import com.aichoice.assistant.databinding.ActivityCalendarBinding;
import com.aichoice.assistant.logic.CalendarMonths;
import com.aichoice.assistant.logic.HolidayCalendar;
import com.aichoice.assistant.logic.RecommendationEngine;
import com.aichoice.assistant.ui.MonthCalendarAdapter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;

/**
 * Month-by-month calendar from the current month to December of the last year
 * that has holiday data. Past months are intentionally not shown.
 */
public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;
    private MonthCalendarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());

        HolidayCalendar calendar = HolidayDataProvider.load(this);
        LocalDate today = LocalDate.now(RecommendationEngine.BEIJING_ZONE);

        HolidayCacheRepository cache = new HolidayCacheRepository(this);
        if (HolidayDataProvider.remoteCalendar(this) != null) {
            binding.tvCalendarSource.setText(getString(
                    R.string.calendar_source_network, cache.getFetchedAt()));
        } else {
            binding.tvCalendarSource.setText(R.string.calendar_source_bundled);
        }

        List<YearMonth> months = CalendarMonths.range(today, calendar.maxYear());
        adapter = new MonthCalendarAdapter();
        adapter.submit(months,
                calendar,
                new HashSet<>(new CustomHolidayRepository(this).getAll().keySet()),
                today);

        binding.recyclerMonths.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMonths.setAdapter(adapter);

        boolean empty = months.isEmpty();
        binding.recyclerMonths.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.tvEmptyCalendar.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
