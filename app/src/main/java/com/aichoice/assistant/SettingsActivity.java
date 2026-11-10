package com.aichoice.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aichoice.assistant.data.CustomHolidayRepository;
import com.aichoice.assistant.data.HolidayAutoRefresher;
import com.aichoice.assistant.data.HolidayCacheRepository;
import com.aichoice.assistant.data.HolidayDataProvider;
import com.aichoice.assistant.data.HolidayRemoteClient;
import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.RuleRepository;
import com.aichoice.assistant.data.Target;
import com.aichoice.assistant.databinding.ActivitySettingsBinding;
import com.aichoice.assistant.databinding.DialogCustomHolidayBinding;
import com.aichoice.assistant.databinding.ItemCustomHolidayBinding;
import com.aichoice.assistant.logic.HolidayCalendar;
import com.aichoice.assistant.logic.Recommendation;
import com.aichoice.assistant.logic.RecommendationEngine;
import com.aichoice.assistant.ui.RuleAdapter;
import com.aichoice.assistant.util.DayLabels;
import com.aichoice.assistant.util.LowPriceFormatter;
import com.aichoice.assistant.util.TimeUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings screen: preview of the current recommendation, full CRUD and
 * reordering for the rules, the countdown duration, and locally managed custom
 * holidays.
 */
public class SettingsActivity extends AppCompatActivity implements RuleAdapter.Listener {

    private ActivitySettingsBinding binding;
    private RuleRepository repository;
    private CustomHolidayRepository customHolidayRepository;
    private HolidayCalendar baseCalendar;
    private HolidayCalendar calendar;
    private RecommendationEngine engine;
    private RuleAdapter adapter;
    private boolean updatingSlider;
    private boolean refreshingHolidays;
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private final Runnable previewTick = new Runnable() {
        @Override
        public void run() {
            refreshPreview();
            previewHandler.postDelayed(this, 30_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new RuleRepository(this);
        customHolidayRepository = new CustomHolidayRepository(this);
        baseCalendar = HolidayDataProvider.baseCalendar(this);
        calendar = HolidayDataProvider.load(this);
        engine = new RecommendationEngine(calendar);

        binding.toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reset_rules) {
                confirmReset();
                return true;
            }
            return false;
        });

        adapter = new RuleAdapter(this);
        adapter.setListener(this);
        binding.recyclerRules.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRules.setAdapter(adapter);

        binding.btnAddRule.setOnClickListener(view ->
                startActivity(new Intent(this, RuleEditActivity.class)));
        binding.btnAddHoliday.setOnClickListener(view -> showDatePicker());
        binding.btnRefreshHoliday.setOnClickListener(view -> refreshHolidays());
        binding.btnOpenCalendar.setOnClickListener(view ->
                startActivity(new Intent(this, CalendarActivity.class)));

        setupCountdownSlider();
        bindHolidayInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadHolidayCalendar();
        refreshRules();
        refreshPreview();
        updateCountdownControls();
        bindHolidayInfo();
        renderCustomHolidays();

        previewHandler.removeCallbacks(previewTick);
        previewHandler.postDelayed(previewTick, 30_000L);

        // Joins the yearly automatic refresh if it is still running, or starts
        // it when the main screen was skipped.
        HolidayAutoRefresher.refreshIfDue(this, this::onHolidayDataRefreshed);
    }

    @Override
    protected void onPause() {
        previewHandler.removeCallbacks(previewTick);
        super.onPause();
    }

    /** Applies a holiday snapshot downloaded in the background. */
    private void onHolidayDataRefreshed() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        reloadHolidayCalendar();
        bindHolidayInfo();
        renderCustomHolidays();
        refreshPreview();
    }

    private void reloadHolidayCalendar() {
        baseCalendar = HolidayDataProvider.baseCalendar(this);
        calendar = HolidayDataProvider.load(this);
        engine = new RecommendationEngine(calendar);
    }

    private void refreshRules() {
        List<Rule> rules = repository.getRules();
        adapter.submit(rules);
        boolean empty = rules.isEmpty();
        binding.tvEmptyRules.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerRules.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void refreshPreview() {
        ZonedDateTime now = ZonedDateTime.now(RecommendationEngine.BEIJING_ZONE);
        List<Rule> rules = repository.getRules();
        Recommendation recommendation = engine.evaluate(now, rules);
        boolean deepSeek = recommendation.getTarget() == Target.DEEPSEEK;

        // Same brand-colored card as the main screen.
        binding.previewCard.setBackgroundResource(deepSeek
                ? R.drawable.bg_reco_card
                : R.drawable.bg_reco_card_workbuddy);
        binding.tvPreviewTarget.setText(recommendation.getTarget().displayName());
        binding.tvPreviewTarget.setTextColor(ContextCompat.getColor(this,
                deepSeek ? R.color.on_brand : R.color.on_workbuddy));
        int secondaryText = ContextCompat.getColor(this,
                deepSeek ? R.color.on_brand_secondary : R.color.on_workbuddy_secondary);
        binding.tvPreviewLabel.setTextColor(secondaryText);
        binding.tvPreviewTime.setTextColor(secondaryText);
        binding.tvPreviewLowPrice.setTextColor(secondaryText);

        binding.tvPreviewTime.setText(getString(R.string.card_clock,
                TimeUtils.formatDate(now),
                TimeUtils.formatClock(now),
                DayLabels.of(this, calendar, now.toLocalDate())));
        binding.tvPreviewLowPrice.setText(
                LowPriceFormatter.describe(this, engine, rules, now, recommendation));
    }

    private void setupCountdownSlider() {
        binding.sliderCountdown.setValueFrom(RuleRepository.MIN_COUNTDOWN_SECONDS);
        binding.sliderCountdown.setValueTo(RuleRepository.MAX_COUNTDOWN_SECONDS);
        binding.sliderCountdown.setStepSize(1f);
        binding.sliderCountdown.setValue(repository.getCountdownSeconds());

        binding.sliderCountdown.addOnChangeListener((slider, value, fromUser) -> {
            int seconds = Math.round(value);
            binding.tvCountdownValue.setText(
                    getString(R.string.settings_countdown_value, seconds));
            if (fromUser && !updatingSlider) {
                repository.setCountdownSeconds(seconds);
            }
        });
    }

    private void updateCountdownControls() {
        int seconds = repository.getCountdownSeconds();
        binding.tvCountdownValue.setText(
                getString(R.string.settings_countdown_value, seconds));
        if (Math.round(binding.sliderCountdown.getValue()) != seconds) {
            updatingSlider = true;
            binding.sliderCountdown.setValue(seconds);
            updatingSlider = false;
        }
    }

    private void bindHolidayInfo() {
        binding.tvHolidayCoverage.setText(
                getString(R.string.settings_holiday_coverage, baseCalendar.yearRange()));
        String updated = baseCalendar.updated().isEmpty()
                ? getString(R.string.settings_holiday_unknown)
                : baseCalendar.updated();
        binding.tvHolidayDesc.setText(getString(R.string.settings_holiday_desc, updated));
        binding.tvVersion.setText(
                getString(R.string.settings_version, BuildConfig.VERSION_NAME, updated));

        HolidayCacheRepository cache = new HolidayCacheRepository(this);
        if (HolidayDataProvider.remoteCalendar(this) != null) {
            binding.tvHolidaySource.setText(getString(
                    R.string.settings_holiday_source_network, cache.getFetchedAt()));
        } else {
            binding.tvHolidaySource.setText(R.string.settings_holiday_source_bundled);
        }
    }

    private void renderCustomHolidays() {
        Map<LocalDate, String> holidays = customHolidayRepository.getAll();
        binding.holidayListContainer.removeAllViews();

        int count = holidays.size();
        binding.tvCustomHolidayCount.setText(
                getString(R.string.settings_custom_holiday_count, count));
        boolean empty = count == 0;
        binding.tvNoCustomHolidays.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.holidayListContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            return;
        }

        List<LocalDate> dates = new ArrayList<>(holidays.keySet());
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            String name = holidays.get(date);
            ItemCustomHolidayBinding item = ItemCustomHolidayBinding.inflate(
                    getLayoutInflater(), binding.holidayListContainer, false);
            item.tvHolidayDate.setText(TimeUtils.formatFullDate(date));
            item.tvHolidayName.setText(name == null || name.trim().isEmpty()
                    ? getString(R.string.custom_holiday_default_name)
                    : name);
            item.divider.setVisibility(i == dates.size() - 1 ? View.GONE : View.VISIBLE);
            item.btnDeleteHoliday.setOnClickListener(view ->
                    confirmDeleteHoliday(date, name));
            binding.holidayListContainer.addView(item.getRoot());
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.dialog_add_holiday_title)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            LocalDate date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            showHolidayNameDialog(date);
        });
        picker.show(getSupportFragmentManager(), "custom_holiday_date");
    }

    private void showHolidayNameDialog(LocalDate date) {
        String existing = customHolidayRepository.getName(date);
        DialogCustomHolidayBinding dialogBinding =
                DialogCustomHolidayBinding.inflate(getLayoutInflater());
        if (existing != null) {
            dialogBinding.etHolidayName.setText(existing);
            dialogBinding.etHolidayName.setSelection(existing.length());
        }

        boolean editing = existing != null;
        new MaterialAlertDialogBuilder(this)
                .setTitle(editing
                        ? R.string.dialog_edit_holiday_title
                        : R.string.dialog_add_holiday_title)
                .setMessage(TimeUtils.formatFullDate(date))
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(editing ? R.string.dialog_save : R.string.dialog_add,
                        (dialog, which) -> {
                            String rawName = dialogBinding.etHolidayName.getText() == null
                                    ? ""
                                    : dialogBinding.etHolidayName.getText().toString();
                            customHolidayRepository.add(date, rawName);
                            onCustomHolidaysChanged();
                        })
                .show();
    }

    private void confirmDeleteHoliday(LocalDate date, String name) {
        String safeName = name == null || name.trim().isEmpty()
                ? getString(R.string.custom_holiday_default_name)
                : name;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_holiday_title)
                .setMessage(getString(R.string.dialog_delete_holiday_message,
                        TimeUtils.formatFullDate(date), safeName))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    customHolidayRepository.remove(date);
                    onCustomHolidaysChanged();
                })
                .show();
    }

    private void onCustomHolidaysChanged() {
        reloadHolidayCalendar();
        renderCustomHolidays();
        refreshPreview();
    }

    /**
     * Downloads the chinese-days snapshot on demand. The UI keeps working with
     * cached or bundled data while the request runs and if it fails.
     */
    private void refreshHolidays() {
        if (refreshingHolidays) {
            return;
        }
        refreshingHolidays = true;
        binding.btnRefreshHoliday.setEnabled(false);
        binding.btnRefreshHoliday.setText(R.string.settings_refreshing);

        refreshExecutor.execute(() -> {
            boolean success = true;
            try {
                String raw = HolidayRemoteClient.fetch();
                HolidayDataProvider.refreshFromNetwork(getApplicationContext(), raw);
            } catch (Exception e) {
                success = false;
            }
            final boolean finishedSuccessfully = success;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                refreshingHolidays = false;
                binding.btnRefreshHoliday.setEnabled(true);
                binding.btnRefreshHoliday.setText(R.string.settings_refresh_holiday);
                if (finishedSuccessfully) {
                    reloadHolidayCalendar();
                    renderCustomHolidays();
                    refreshPreview();
                    bindHolidayInfo();
                    Toast.makeText(this, R.string.settings_refresh_success,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.settings_refresh_failed,
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        refreshExecutor.shutdownNow();
        super.onDestroy();
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.settings_reset_confirm, (dialog, which) -> {
                    repository.resetToDefaults();
                    refreshRules();
                    refreshPreview();
                })
                .show();
    }

    @Override
    public void onRuleEnabledChanged(Rule rule, boolean enabled) {
        rule.setEnabled(enabled);
        repository.update(rule);
        refreshPreview();
        refreshRules();
    }

    @Override
    public void onRuleEdit(Rule rule) {
        Intent intent = new Intent(this, RuleEditActivity.class);
        intent.putExtra(RuleEditActivity.EXTRA_RULE_ID, rule.getId());
        startActivity(intent);
    }

    @Override
    public void onRuleDelete(Rule rule) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rule_delete_title)
                .setMessage(R.string.rule_delete_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    repository.delete(rule.getId());
                    refreshRules();
                    refreshPreview();
                })
                .show();
    }

    @Override
    public void onRuleMove(Rule rule, int delta) {
        repository.move(rule.getId(), delta);
        refreshRules();
        refreshPreview();
    }
}
