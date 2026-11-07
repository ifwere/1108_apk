package com.aichoice.assistant.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON codec for the user-defined holiday list. Kept free of Android APIs so it
 * can be unit tested on the JVM.
 */
public final class CustomHolidayCodec {

    private CustomHolidayCodec() {
    }

    public static String encode(Map<LocalDate, String> holidays) {
        JSONArray array = new JSONArray();
        if (holidays != null) {
            for (Map.Entry<LocalDate, String> entry : new TreeMap<>(holidays).entrySet()) {
                JSONObject object = new JSONObject();
                try {
                    object.put("date", entry.getKey().toString());
                    object.put("name", entry.getValue() == null ? "" : entry.getValue());
                    array.put(object);
                } catch (JSONException ignored) {
                    // Skip an entry that cannot be serialized.
                }
            }
        }
        return array.toString();
    }

    public static Map<LocalDate, String> decode(String raw) {
        Map<LocalDate, String> result = new TreeMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String rawDate = object.optString("date", "");
                if (rawDate.isEmpty()) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(rawDate);
                    result.put(date, object.optString("name", ""));
                } catch (Exception ignored) {
                    // Ignore malformed dates instead of losing the whole list.
                }
            }
        } catch (JSONException ignored) {
            // Return whatever could be parsed; an empty map is a safe fallback.
        }
        return result;
    }
}
