package com.aichoice.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aichoice.assistant.data.CustomHolidayRepository;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new RuleRepository(this);
        customHolidayRepository = new CustomHolidayRepository(this);
        baseCalendar = HolidayCalendar.fromAssets(this);
        calendar = baseCalendar;
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
    }

    private void reloadHolidayCalendar() {
        calendar = baseCalendar.withAdditionalHolidays(customHolidayRepository.getAll());
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
        Recommendation recommendation = engine.evaluate(now, repository.getRules());

        binding.tvPreviewTarget.setText(recommendation.getTarget().displayName());
        binding.tvPreviewTarget.setTextColor(ContextCompat.getColor(this,
                recommendation.getTarget() == Target.DEEPSEEK
                        ? R.color.ring_deepseek
                        : R.color.ring_workbuddy));

        String ruleName = recommendation.getRuleName();
        if (ruleName == null || ruleName.trim().isEmpty()) {
            ruleName = getString(R.string.rule_untitled);
        }
        String ruleText = recommendation.isFallback()
                ? getString(R.string.settings_preview_default_rule)
                : getString(R.string.settings_preview_rule, ruleName);
        binding.tvPreviewRule.setText(ruleText);
        binding.tvPreviewTime.setText(TimeUtils.formatDate(now)
                + " " + TimeUtils.formatClock(now));
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
