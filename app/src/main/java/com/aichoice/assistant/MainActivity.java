package com.aichoice.assistant;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aichoice.assistant.data.CustomHolidayRepository;
import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.RuleRepository;
import com.aichoice.assistant.data.Target;
import com.aichoice.assistant.databinding.ActivityMainBinding;
import com.aichoice.assistant.logic.DayKind;
import com.aichoice.assistant.logic.HolidayCalendar;
import com.aichoice.assistant.logic.Recommendation;
import com.aichoice.assistant.logic.RecommendationEngine;
import com.aichoice.assistant.util.TimeUtils;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Entry point. Shows the recommendation for the current Beijing time in the
 * upper half of the screen and exits automatically when the countdown ends.
 * The lower half offers a way to stop the timer and open the settings.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RuleRepository repository;
    private CustomHolidayRepository customHolidayRepository;
    private HolidayCalendar baseCalendar;
    private HolidayCalendar calendar;
    private RecommendationEngine engine;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = this::tickCountdown;
    private final Runnable exitRunnable = this::exitApp;

    private boolean countdownActive;
    private boolean navigatingToSettings;
    private long countdownEndRealtime;
    private long countdownTotalMillis;
    private int lastShownSecond = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new RuleRepository(this);
        customHolidayRepository = new CustomHolidayRepository(this);
        baseCalendar = HolidayCalendar.fromAssets(this);
        calendar = baseCalendar;
        engine = new RecommendationEngine(calendar);

        binding.btnSettings.setOnClickListener(view -> openSettings());
        // The whole lower half is one large tap target: it stops the timer and
        // opens settings, exactly like the visible button.
        binding.bottomSection.setOnClickListener(view -> openSettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigatingToSettings = false;
        // Reload in case the user edited rules, countdown or custom holidays.
        calendar = baseCalendar.withAdditionalHolidays(customHolidayRepository.getAll());
        engine = new RecommendationEngine(calendar);
        ZonedDateTime now = ZonedDateTime.now(RecommendationEngine.BEIJING_ZONE);
        List<Rule> rules = repository.getRules();
        Recommendation recommendation = engine.evaluate(now, rules);
        bindRecommendation(recommendation);
        updateClock(now);
        startCountdown(repository.getCountdownSeconds());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCountdown();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void bindRecommendation(Recommendation recommendation) {
        Target target = recommendation.getTarget();
        boolean deepSeek = target == Target.DEEPSEEK;

        binding.tvTarget.setText(target.displayName());
        binding.tvBadge.setText(target.shortName());

        int accent = ContextCompat.getColor(this,
                deepSeek ? R.color.deepseek : R.color.workbuddy);
        int ringColor = ContextCompat.getColor(this,
                deepSeek ? R.color.ring_light_deepseek : R.color.ring_light_workbuddy);
        binding.tvBadge.setBackgroundTintList(ColorStateList.valueOf(accent));
        binding.ring.setAccentColor(ringColor);

        String ruleName = recommendation.getRuleName();
        if (ruleName == null || ruleName.trim().isEmpty()) {
            ruleName = getString(R.string.rule_untitled);
        }
        String ruleText = recommendation.isFallback()
                ? getString(R.string.rule_default_hit)
                : getString(R.string.rule_hit, ruleName);
        binding.tvRuleHit.setText(ruleText);

        // Gentle entrance animation every time the screen is refreshed.
        binding.tvTarget.animate().cancel();
        binding.tvTarget.setAlpha(0f);
        binding.tvTarget.setTranslationY(dp(8));
        binding.tvTarget.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        binding.tvBadge.animate().cancel();
        binding.tvBadge.setAlpha(0f);
        binding.tvBadge.animate().alpha(1f).setDuration(220).start();
    }

    private void updateClock(ZonedDateTime now) {
        ZonedDateTime beijingTime = now.withZoneSameInstant(RecommendationEngine.BEIJING_ZONE);
        DayKind kind = calendar.classify(beijingTime.toLocalDate());
        String dayLabel;
        if (kind == DayKind.HOLIDAY) {
            String name = calendar.holidayName(beijingTime.toLocalDate());
            dayLabel = name == null ? getString(R.string.day_holiday) : name;
        } else if (kind == DayKind.WEEKEND) {
            dayLabel = getString(R.string.day_weekend);
        } else {
            dayLabel = getString(R.string.day_workday);
        }
        binding.tvClock.setText(getString(R.string.main_clock,
                TimeUtils.formatDate(beijingTime),
                TimeUtils.formatClock(beijingTime),
                dayLabel));
    }

    private void startCountdown(int seconds) {
        stopCountdown();
        int safeSeconds = Math.max(1, seconds);
        countdownTotalMillis = safeSeconds * 1000L;
        countdownEndRealtime = SystemClock.elapsedRealtime() + countdownTotalMillis;
        countdownActive = true;
        lastShownSecond = safeSeconds;

        binding.ring.setProgress(1f);
        binding.tvRemaining.setText(String.valueOf(safeSeconds));
        binding.tvCountdownHint.setText(getString(R.string.main_countdown_hint, safeSeconds));
        binding.btnSettings.setEnabled(true);
        handler.post(tickRunnable);
    }

    private void tickCountdown() {
        if (!countdownActive) {
            return;
        }
        long remaining = countdownEndRealtime - SystemClock.elapsedRealtime();
        if (remaining <= 0) {
            countdownActive = false;
            binding.ring.setProgress(0f);
            binding.tvRemaining.setText("0");
            binding.tvCountdownHint.setText(R.string.main_countdown_exiting);
            handler.postDelayed(exitRunnable, 250);
            return;
        }

        binding.ring.setProgress(remaining / (float) countdownTotalMillis);
        int seconds = (int) Math.ceil(remaining / 1000.0);
        if (seconds != lastShownSecond) {
            lastShownSecond = seconds;
            binding.tvRemaining.setText(String.valueOf(seconds));
            binding.tvCountdownHint.setText(getString(R.string.main_countdown_hint, seconds));
            pulseRemaining();
            updateClock(ZonedDateTime.now(RecommendationEngine.BEIJING_ZONE));
        }
        handler.postDelayed(tickRunnable, 50);
    }

    private void pulseRemaining() {
        binding.tvRemaining.animate().cancel();
        binding.tvRemaining.setScaleX(1.06f);
        binding.tvRemaining.setScaleY(1.06f);
        binding.tvRemaining.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start();
    }

    private void stopCountdown() {
        countdownActive = false;
        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(exitRunnable);
    }

    private void openSettings() {
        if (navigatingToSettings) {
            return;
        }
        navigatingToSettings = true;
        stopCountdown();
        binding.btnSettings.setEnabled(false);
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void exitApp() {
        if (!isFinishing()) {
            finishAndRemoveTask();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
