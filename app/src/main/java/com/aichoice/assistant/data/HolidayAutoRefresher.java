package com.aichoice.assistant.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.aichoice.assistant.logic.HolidayAutoRefreshPolicy;
import com.aichoice.assistant.logic.RecommendationEngine;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes the holiday snapshot from the network once a year, from December
 * 20 onwards. The check runs when the app opens; a failed attempt is retried
 * the next day. Success and failure are silent: the UI keeps using cached or
 * bundled data.
 */
public final class HolidayAutoRefresher {

    private static final String PREFS_NAME = "ai_choice_holiday_auto";
    private static final String KEY_LAST_ATTEMPT = "last_attempt_date_v1";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "holiday-auto-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final List<Runnable> PENDING_CALLBACKS = new ArrayList<>();

    private HolidayAutoRefresher() {
    }

    /**
     * Starts an automatic refresh when the policy says one is due.
     *
     * @param onFinished invoked on the main thread after a successful refresh,
     *                   so an open screen can reload its data
     * @return true when a refresh is due (a request was started or joined)
     */
    public static boolean refreshIfDue(Context context, Runnable onFinished) {
        Context appContext = context.getApplicationContext();
        LocalDate today = ZonedDateTime.now(RecommendationEngine.BEIJING_ZONE).toLocalDate();
        SharedPreferences preferences = appContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        LocalDate lastAttempt = parseDate(
                preferences.getString(KEY_LAST_ATTEMPT, ""));
        LocalDate lastSuccess = parseDate(
                new HolidayCacheRepository(appContext).getFetchedAt());

        if (!HolidayAutoRefreshPolicy.shouldRefresh(today, lastAttempt, lastSuccess)) {
            return false;
        }

        synchronized (PENDING_CALLBACKS) {
            if (onFinished != null) {
                PENDING_CALLBACKS.add(onFinished);
            }
        }
        if (RUNNING.compareAndSet(false, true)) {
            EXECUTOR.execute(() -> run(appContext, today));
        }
        return true;
    }

    private static void run(Context appContext, LocalDate attemptedOn) {
        boolean success = false;
        try {
            String raw = HolidayRemoteClient.fetch();
            HolidayDataProvider.refreshFromNetwork(appContext, raw);
            success = true;
        } catch (Exception ignored) {
            // Keep the last cached snapshot; retry on the next day.
        }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_ATTEMPT, attemptedOn.format(DATE_FORMAT))
                .apply();

        List<Runnable> callbacks = new ArrayList<>();
        if (success) {
            synchronized (PENDING_CALLBACKS) {
                callbacks.addAll(PENDING_CALLBACKS);
                PENDING_CALLBACKS.clear();
            }
        } else {
            synchronized (PENDING_CALLBACKS) {
                PENDING_CALLBACKS.clear();
            }
        }
        RUNNING.set(false);

        if (!callbacks.isEmpty()) {
            MAIN_HANDLER.post(() -> {
                for (Runnable callback : callbacks) {
                    callback.run();
                }
            });
        }
    }

    /** Parses "yyyy-MM-dd HH:mm" or "yyyy-MM-dd"; null when empty/invalid. */
    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        String datePart = trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
        try {
            return LocalDate.parse(datePart, DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
