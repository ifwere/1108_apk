package com.aichoice.assistant.logic;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Chinese statutory holiday / official day-off calendar.
 *
 * <p>Data is embedded in {@code assets/holidays.json}. Only days off are
 * listed: the {@code "days"} array contains dates that are official holidays
 * (including the days of an extended vacation period). Make-up workdays on a
 * weekend are intentionally not listed, because the product requirement says
 * weekends are always idle.</p>
 */
public final class HolidayCalendar {

    private static final String TAG = "HolidayCalendar";
    public static final String ASSET_NAME = "holidays.json";

    private final Map<LocalDate, String> holidays;
    private final TreeSet<Integer> years;
    private final String updated;

    private HolidayCalendar(Map<LocalDate, String> holidays, String updated) {
        this.holidays = holidays;
        this.years = new TreeSet<>();
        for (LocalDate date : holidays.keySet()) {
            years.add(date.getYear());
        }
        this.updated = updated;
    }

    public static HolidayCalendar empty() {
        return new HolidayCalendar(new HashMap<>(), "");
    }

    public static HolidayCalendar fromAssets(Context context) {
        try (InputStream input = context.getAssets().open(ASSET_NAME)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return fromJson(output.toString(StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            Log.e(TAG, "Unable to load " + ASSET_NAME, e);
            return empty();
        }
    }

    public static HolidayCalendar fromJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        Map<LocalDate, String> map = new HashMap<>();
        JSONArray days = root.optJSONArray("days");
        if (days != null) {
            for (int i = 0; i < days.length(); i++) {
                JSONObject day = days.optJSONObject(i);
                if (day == null) {
                    continue;
                }
                String rawDate = day.optString("date", "");
                if (rawDate.isEmpty()) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(rawDate);
                    String name = day.optString("name", "法定节假日");
                    map.put(date, name);
                } catch (Exception ignored) {
                    Log.w(TAG, "Skipping malformed holiday date: " + rawDate);
                }
            }
        }
        return new HolidayCalendar(map, root.optString("updated", ""));
    }

    /**
     * Returns a new calendar with user-defined holidays merged over the
     * built-in data. A custom entry replaces the name of a built-in holiday on
     * the same date and can also add dates outside the bundled years.
     */
    public HolidayCalendar withAdditionalHolidays(Map<LocalDate, String> additional) {
        if (additional == null || additional.isEmpty()) {
            return this;
        }
        Map<LocalDate, String> merged = new HashMap<>(holidays);
        for (Map.Entry<LocalDate, String> entry : additional.entrySet()) {
            LocalDate date = entry.getKey();
            if (date == null) {
                continue;
            }
            String name = entry.getValue();
            if (name == null || name.trim().isEmpty()) {
                name = "自定义节假日";
            }
            merged.put(date, name);
        }
        return new HolidayCalendar(merged, updated);
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.containsKey(date);
    }

    /** Holiday name, or {@code null} when the date is not a holiday. */
    public String holidayName(LocalDate date) {
        return holidays.get(date);
    }

    public DayKind classify(LocalDate date) {
        if (holidays.containsKey(date)) {
            return DayKind.HOLIDAY;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayKind.WEEKEND;
        }
        return DayKind.WORKDAY;
    }

    public int firstYear() {
        return years.isEmpty() ? 0 : years.first();
    }

    public int lastYear() {
        return years.isEmpty() ? 0 : years.last();
    }

    public String yearRange() {
        if (years.isEmpty()) {
            return "—";
        }
        int first = firstYear();
        int last = lastYear();
        return first == last ? String.valueOf(first) : first + "–" + last;
    }

    public String updated() {
        return updated;
    }
}
