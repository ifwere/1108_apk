package com.aichoice.assistant.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stores user-defined holidays (extra rest days) locally. They are merged over
 * the built-in calendar, so they work for company holidays and for future
 * years whose official schedule has not been bundled into the app yet.
 */
public final class CustomHolidayRepository {

    private static final String PREFS_NAME = "ai_choice_custom_holidays";
    private static final String KEY_DAYS = "days_v1";
    public static final String DEFAULT_NAME = "自定义节假日";
    private static final int MAX_NAME_LENGTH = 40;

    private final SharedPreferences preferences;

    public CustomHolidayRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** All custom holidays, sorted by date. */
    public synchronized Map<LocalDate, String> getAll() {
        Map<LocalDate, String> decoded =
                CustomHolidayCodec.decode(preferences.getString(KEY_DAYS, ""));
        return new TreeMap<>(decoded);
    }

    public synchronized String getName(LocalDate date) {
        return getAll().get(date);
    }

    public synchronized int size() {
        return getAll().size();
    }

    /** Adds a holiday, or updates the name if the date already exists. */
    public synchronized void add(LocalDate date, String name) {
        if (date == null) {
            return;
        }
        Map<LocalDate, String> all = getAll();
        String safeName = name == null ? "" : name.trim();
        if (safeName.isEmpty()) {
            safeName = DEFAULT_NAME;
        }
        if (safeName.length() > MAX_NAME_LENGTH) {
            safeName = safeName.substring(0, MAX_NAME_LENGTH);
        }
        all.put(date, safeName);
        save(all);
    }

    public synchronized void remove(LocalDate date) {
        Map<LocalDate, String> all = getAll();
        if (all.remove(date) != null) {
            save(all);
        }
    }

    private void save(Map<LocalDate, String> all) {
        preferences.edit()
                .putString(KEY_DAYS, CustomHolidayCodec.encode(all))
                .apply();
    }
}
