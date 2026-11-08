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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Chinese statutory holiday / official day-off calendar.
 *
 * <p>Two data formats are supported:</p>
 * <ul>
 *   <li>Bundled asset format: {@code {"days":[{"date":..,"name":..}], "workdays":[...]}}</li>
 *   <li>chinese-days format:
 *       {@code {"holidays":{"2026-10-01":"National Day,国庆节,3"},
 *       "workdays":{"2026-10-10":"National Day,国庆节,3"}}}</li>
 * </ul>
 *
 * <p>Only days off affect the recommendation engine. Make-up workdays that fall
 * on a weekend intentionally stay weekends: the requested rule says weekdays
 * are peak time, so a Saturday or Sunday is always idle even when it is a
 * make-up workday. The make-up data is kept for calendar labels only.</p>
 */
public final class HolidayCalendar {

    private static final String TAG = "HolidayCalendar";
    public static final String ASSET_NAME = "holidays.json";
    public static final String DEFAULT_HOLIDAY_NAME = "法定节假日";
    public static final String DEFAULT_MAKEUP_NAME = "调休上班";

    private final Map<LocalDate, String> holidays;
    private final Map<LocalDate, String> makeupWorkdays;
    private final TreeSet<Integer> years;
    private final String updated;

    private HolidayCalendar(Map<LocalDate, String> holidays,
                            Map<LocalDate, String> makeupWorkdays,
                            String updated) {
        this.holidays = holidays;
        this.makeupWorkdays = makeupWorkdays;
        this.years = new TreeSet<>();
        for (LocalDate date : holidays.keySet()) {
            years.add(date.getYear());
        }
        for (LocalDate date : makeupWorkdays.keySet()) {
            years.add(date.getYear());
        }
        this.updated = updated == null ? "" : updated;
    }

    public static HolidayCalendar empty() {
        return new HolidayCalendar(new HashMap<>(), new HashMap<>(), "");
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

    /**
     * Parses either the bundled asset format or the chinese-days format. The
     * format is detected from the top-level JSON object.
     */
    public static HolidayCalendar fromJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        Map<LocalDate, String> holidayMap = new HashMap<>();
        Map<LocalDate, String> makeupMap = new HashMap<>();

        JSONObject remoteHolidays = root.optJSONObject("holidays");
        if (remoteHolidays != null) {
            parseObjectMap(remoteHolidays, holidayMap, DEFAULT_HOLIDAY_NAME);
            parseObjectMap(root.optJSONObject("workdays"), makeupMap, DEFAULT_MAKEUP_NAME);
        } else {
            parseDateArray(root.optJSONArray("days"), holidayMap, DEFAULT_HOLIDAY_NAME);
            JSONArray workdayArray = root.optJSONArray("workdays");
            if (workdayArray != null) {
                parseDateArray(workdayArray, makeupMap, DEFAULT_MAKEUP_NAME);
            } else {
                parseObjectMap(root.optJSONObject("workdays"), makeupMap, DEFAULT_MAKEUP_NAME);
            }
        }

        return new HolidayCalendar(holidayMap, makeupMap, root.optString("updated", ""));
    }

    /** Explicit alias used by the network refresh flow. */
    public static HolidayCalendar fromChineseDaysJson(String json) throws Exception {
        return fromJson(json);
    }

    private static void parseObjectMap(JSONObject object,
                                       Map<LocalDate, String> target,
                                       String fallbackName) {
        if (object == null) {
            return;
        }
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String rawDate = keys.next();
            String rawValue = object.optString(rawDate, "");
            LocalDate date = parseDate(rawDate);
            if (date == null) {
                continue;
            }
            target.put(date, chineseName(rawValue, fallbackName));
        }
    }

    private static void parseDateArray(JSONArray array,
                                       Map<LocalDate, String> target,
                                       String fallbackName) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject day = array.optJSONObject(i);
            if (day == null) {
                continue;
            }
            LocalDate date = parseDate(day.optString("date", ""));
            if (date == null) {
                continue;
            }
            String name = day.optString("name", "");
            if (name.trim().isEmpty()) {
                name = fallbackName;
            }
            target.put(date, name);
        }
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception ignored) {
            Log.w(TAG, "Skipping malformed date: " + raw);
            return null;
        }
    }

    /**
     * chinese-days values look like {@code "National Day,国庆节,3"}. The second
     * comma-separated field is the Chinese name.
     */
    private static String chineseName(String rawValue, String fallbackName) {
        if (rawValue == null) {
            return fallbackName;
        }
        String[] parts = rawValue.split(",", -1);
        if (parts.length >= 2) {
            String name = parts[1].trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        String whole = rawValue.trim();
        return whole.isEmpty() ? fallbackName : whole;
    }

    /**
     * Returns a new calendar with user-defined holidays merged over the
     * built-in and network data. A custom entry replaces the name of a holiday
     * on the same date and can also add dates outside the bundled years.
     */
    public HolidayCalendar withAdditionalHolidays(Map<LocalDate, String> additional) {
        if (additional == null || additional.isEmpty()) {
            return this;
        }
        Map<LocalDate, String> merged = new HashMap<>(holidays);
        Map<LocalDate, String> mergedMakeup = new HashMap<>(makeupWorkdays);
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
            // A user-defined rest day wins over a network make-up workday.
            mergedMakeup.remove(date);
        }
        return new HolidayCalendar(merged, mergedMakeup, updated);
    }

    /** Returns a copy with a different metadata timestamp. */
    public HolidayCalendar withUpdated(String newUpdated) {
        return new HolidayCalendar(new HashMap<>(holidays),
                new HashMap<>(makeupWorkdays), newUpdated);
    }

    /**
     * Merges this calendar over {@code fallback}. Every year present in this
     * calendar is removed from the fallback first, so newer official data
     * always replaces stale bundled entries instead of being unioned with
     * them. Years unknown to this calendar keep their fallback entries.
     */
    public HolidayCalendar mergeOver(HolidayCalendar fallback) {
        if (fallback == null) {
            return this;
        }
        if (holidays.isEmpty() && makeupWorkdays.isEmpty()) {
            return fallback;
        }
        // Only years with holiday data can replace the fallback rest days. A
        // make-up workday entry alone does not prove the holiday list for that
        // year is known.
        TreeSet<Integer> holidayYears = new TreeSet<>();
        for (LocalDate date : holidays.keySet()) {
            holidayYears.add(date.getYear());
        }
        TreeSet<Integer> makeupYears = new TreeSet<>(holidayYears);
        for (LocalDate date : makeupWorkdays.keySet()) {
            makeupYears.add(date.getYear());
        }

        Map<LocalDate, String> mergedHolidays = new HashMap<>();
        Map<LocalDate, String> mergedMakeup = new HashMap<>();
        for (Map.Entry<LocalDate, String> entry : fallback.holidays.entrySet()) {
            if (!holidayYears.contains(entry.getKey().getYear())) {
                mergedHolidays.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<LocalDate, String> entry : fallback.makeupWorkdays.entrySet()) {
            if (!makeupYears.contains(entry.getKey().getYear())) {
                mergedMakeup.put(entry.getKey(), entry.getValue());
            }
        }
        mergedHolidays.putAll(holidays);
        mergedMakeup.putAll(makeupWorkdays);
        String mergedUpdated = updated.isEmpty() ? fallback.updated : updated;
        return new HolidayCalendar(mergedHolidays, mergedMakeup, mergedUpdated);
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.containsKey(date);
    }

    /** Holiday name, or {@code null} when the date is not a holiday. */
    public String holidayName(LocalDate date) {
        return holidays.get(date);
    }

    public boolean isMakeupWorkday(LocalDate date) {
        return makeupWorkdays.containsKey(date);
    }

    /** Make-up workday name (usually the festival it belongs to), or null. */
    public String makeupWorkdayName(LocalDate date) {
        return makeupWorkdays.get(date);
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

    public int maxYear() {
        return lastYear();
    }

    public boolean hasYear(int year) {
        return years.contains(year);
    }

    public String yearRange() {
        if (years.isEmpty()) {
            return "-";
        }
        int first = firstYear();
        int last = lastYear();
        return first == last ? String.valueOf(first) : first + "-" + last;
    }

    public String updated() {
        return updated;
    }
}
