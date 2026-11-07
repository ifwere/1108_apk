package com.aichoice.assistant;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.RuleRepository;
import com.aichoice.assistant.data.Target;
import com.aichoice.assistant.databinding.ActivityRuleEditBinding;
import com.aichoice.assistant.util.TimeUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * Create or edit one rule. Supports name, target tool, weekdays, time range,
 * statutory-holiday skipping and the enabled flag.
 */
public class RuleEditActivity extends AppCompatActivity {

    public static final String EXTRA_RULE_ID = "extra_rule_id";

    private ActivityRuleEditBinding binding;
    private RuleRepository repository;
    private Rule editingRule;
    private boolean editingExisting;
    private int startMinute;
    private int endMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRuleEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new RuleRepository(this);
        String ruleId = getIntent().getStringExtra(EXTRA_RULE_ID);
        Rule existing = ruleId == null ? null : repository.findById(ruleId);
        editingExisting = existing != null;
        editingRule = editingExisting ? existing.copy() : new Rule();

        binding.toolbar.setTitle(editingExisting
                ? R.string.rule_edit_title
                : R.string.rule_new_title);
        binding.toolbar.setNavigationOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());

        startMinute = editingRule.getStartMinute();
        endMinute = editingRule.getEndMinute();

        populateForm();
        wireInteractions();
    }

    private void populateForm() {
        binding.etName.setText(editingRule.getName());
        binding.targetGroup.check(editingRule.getTarget() == Target.WORKBUDDY
                ? R.id.btnTargetWorkbuddy
                : R.id.btnTargetDeepseek);

        Chip[] chips = dayChips();
        for (int i = 0; i < chips.length; i++) {
            chips[i].setChecked(editingRule.isDaySelected(i));
        }

        binding.switchAllDay.setChecked(editingRule.isAllDay());
        binding.switchSkipHolidays.setChecked(editingRule.isSkipHolidays());
        binding.switchEnabled.setChecked(editingRule.isEnabled());
        binding.btnDelete.setVisibility(editingExisting ? android.view.View.VISIBLE : android.view.View.GONE);
        updateTimeButtons();
    }

    private void wireInteractions() {
        binding.switchAllDay.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateTimeButtons());

        binding.btnStartTime.setOnClickListener(view -> showTimePicker(true));
        binding.btnEndTime.setOnClickListener(view -> showTimePicker(false));

        binding.btnPresetWeekdays.setOnClickListener(view -> applyDays(
                new boolean[]{true, true, true, true, true, false, false}));
        binding.btnPresetWeekend.setOnClickListener(view -> applyDays(
                new boolean[]{false, false, false, false, false, true, true}));
        binding.btnPresetEveryday.setOnClickListener(view -> applyDays(
                new boolean[]{true, true, true, true, true, true, true}));

        binding.btnSave.setOnClickListener(view -> saveRule());
        binding.btnDelete.setOnClickListener(view -> confirmDelete());
    }

    private void updateTimeButtons() {
        boolean allDay = binding.switchAllDay.isChecked();
        binding.btnStartTime.setEnabled(!allDay);
        binding.btnEndTime.setEnabled(!allDay);
        binding.btnStartTime.setAlpha(allDay ? 0.45f : 1f);
        binding.btnEndTime.setAlpha(allDay ? 0.45f : 1f);
        binding.btnStartTime.setText(allDay ? "00:00" : TimeUtils.formatMinute(startMinute));
        binding.btnEndTime.setText(allDay ? "24:00" : TimeUtils.formatEnd(endMinute));
        binding.tvTimeHint.setAlpha(allDay ? 0.55f : 1f);
    }

    private void showTimePicker(boolean isStart) {
        int current = isStart ? startMinute : endMinute;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current / 60)
                .setMinute(current % 60)
                .setTitleText(isStart
                        ? R.string.rule_time_start_title
                        : R.string.rule_time_end_title)
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            int minutes = picker.getHour() * 60 + picker.getMinute();
            if (isStart) {
                startMinute = minutes;
            } else {
                endMinute = minutes;
            }
            updateTimeButtons();
        });
        picker.show(getSupportFragmentManager(), isStart ? "start_time" : "end_time");
    }

    private void applyDays(boolean[] selected) {
        Chip[] chips = dayChips();
        for (int i = 0; i < chips.length; i++) {
            chips[i].setChecked(selected[i]);
        }
    }

    private void saveRule() {
        String name = binding.etName.getText() == null
                ? ""
                : binding.etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.rule_name_required));
            binding.etName.requestFocus();
            return;
        }
        binding.tilName.setError(null);

        boolean[] days = new boolean[Rule.DAY_COUNT];
        int selectedDays = 0;
        Chip[] chips = dayChips();
        for (int i = 0; i < chips.length; i++) {
            days[i] = chips[i].isChecked();
            if (days[i]) {
                selectedDays++;
            }
        }
        if (selectedDays == 0) {
            Snackbar.make(binding.getRoot(), R.string.rule_days_required,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        boolean allDay = binding.switchAllDay.isChecked();
        if (!allDay && startMinute == endMinute) {
            Snackbar.make(binding.getRoot(), R.string.rule_time_equal_error,
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        editingRule.setName(name);
        editingRule.setDays(days);
        if (allDay) {
            editingRule.setStartMinute(0);
            editingRule.setEndMinute(0);
        } else {
            editingRule.setStartMinute(startMinute);
            editingRule.setEndMinute(endMinute);
        }
        editingRule.setTarget(binding.targetGroup.getCheckedButtonId() == R.id.btnTargetWorkbuddy
                ? Target.WORKBUDDY
                : Target.DEEPSEEK);
        editingRule.setSkipHolidays(binding.switchSkipHolidays.isChecked());
        editingRule.setEnabled(binding.switchEnabled.isChecked());

        if (editingExisting) {
            repository.update(editingRule);
        } else {
            repository.add(editingRule);
        }
        finish();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rule_delete_title)
                .setMessage(R.string.rule_delete_message)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    repository.delete(editingRule.getId());
                    finish();
                })
                .show();
    }

    private Chip[] dayChips() {
        return new Chip[]{
                binding.chipMon,
                binding.chipTue,
                binding.chipWed,
                binding.chipThu,
                binding.chipFri,
                binding.chipSat,
                binding.chipSun
        };
    }
}
