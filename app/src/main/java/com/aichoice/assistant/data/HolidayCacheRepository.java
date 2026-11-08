package com.aichoice.assistant.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Caches the last successfully downloaded chinese-days JSON. Data is kept as a
 * single snapshot and overwritten on every manual refresh; nothing is deleted
 * automatically.
 */
public final class HolidayCacheRepository {

    private static final String PREFS_NAME = "ai_choice_holiday_cache";
    private static final String KEY_JSON = "remote_json_v1";
    private static final String KEY_FETCHED_AT = "fetched_at_v1";

    private final SharedPreferences preferences;

    public HolidayCacheRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Raw downloaded JSON, or null when no successful refresh exists. */
    public String getRawJson() {
        String raw = preferences.getString(KEY_JSON, null);
        return raw == null || raw.isEmpty() ? null : raw;
    }

    /** Human-readable Beijing-time refresh timestamp, or empty. */
    public String getFetchedAt() {
        return preferences.getString(KEY_FETCHED_AT, "");
    }

    public boolean hasCache() {
        return getRawJson() != null;
    }

    public void save(String rawJson, String fetchedAt) {
        preferences.edit()
                .putString(KEY_JSON, rawJson)
                .putString(KEY_FETCHED_AT, fetchedAt)
                .apply();
    }
}
