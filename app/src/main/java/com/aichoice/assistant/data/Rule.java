package com.aichoice.assistant.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * One configurable recommendation rule.
 *
 * <p>A rule matches when it is enabled, the current weekday is selected, the
 * current minute is inside [startMinute, endMinute) (or the rule is an
 * all-day rule), and — when {@link #isSkipHolidays()} is set — the current
 * date is not a Chinese statutory holiday. The first matching rule in list
 * order wins.</p>
 */
public final class Rule {

    /** Index 0 = Monday ... index 6 = Sunday. */
    public static final int DAY_COUNT = 7;

    private String id;
    private String name;
    private boolean enabled;
    private final boolean[] days = new boolean[DAY_COUNT];
    /** Minutes from midnight, [0, 1439]. */
    private int startMinute;
    /** Minutes from midnight, [0, 1439]. Equal to startMinute means "all day". */
    private int endMinute;
    private Target target;
    private boolean skipHolidays;

    public Rule() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.enabled = true;
        // Sensible defaults for a newly created rule: weekday peak hours.
        this.days[0] = true;
        this.days[1] = true;
        this.days[2] = true;
        this.days[3] = true;
        this.days[4] = true;
        this.startMinute = 9 * 60;
        this.endMinute = 12 * 60;
        this.target = Target.WORKBUDDY;
        this.skipHolidays = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && !id.isEmpty()) {
            this.id = id;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean[] getDays() {
        return days;
    }

    public void setDays(boolean[] source) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < DAY_COUNT; i++) {
            days[i] = i < source.length && source[i];
        }
    }

    public void setDay(int index, boolean selected) {
        if (index >= 0 && index < DAY_COUNT) {
            days[index] = selected;
        }
    }

    public boolean isDaySelected(int index) {
        return index >= 0 && index < DAY_COUNT && days[index];
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = clampMinute(startMinute);
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = clampMinute(endMinute);
    }

    public boolean isAllDay() {
        return startMinute == endMinute;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target == null ? Target.DEEPSEEK : target;
    }

    public boolean isSkipHolidays() {
        return skipHolidays;
    }

    public void setSkipHolidays(boolean skipHolidays) {
        this.skipHolidays = skipHolidays;
    }

    public Rule copy() {
        Rule copy = new Rule();
        copy.id = id;
        copy.name = name;
        copy.enabled = enabled;
        System.arraycopy(days, 0, copy.days, 0, DAY_COUNT);
        copy.startMinute = startMinute;
        copy.endMinute = endMinute;
        copy.target = target;
        copy.skipHolidays = skipHolidays;
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("enabled", enabled);
        JSONArray daysArray = new JSONArray();
        for (boolean day : days) {
            daysArray.put(day);
        }
        object.put("days", daysArray);
        object.put("start", startMinute);
        object.put("end", endMinute);
        object.put("target", target.name());
        object.put("skipHolidays", skipHolidays);
        return object;
    }

    public static Rule fromJson(JSONObject object) {
        Rule rule = new Rule();
        String parsedId = object.optString("id", "");
        if (!parsedId.isEmpty()) {
            rule.id = parsedId;
        }
        rule.name = object.optString("name", "");
        rule.enabled = object.optBoolean("enabled", true);

        JSONArray daysArray = object.optJSONArray("days");
        if (daysArray != null) {
            for (int i = 0; i < DAY_COUNT; i++) {
                rule.days[i] = daysArray.optBoolean(i, false);
            }
        }
        rule.startMinute = clampMinute(object.optInt("start", 9 * 60));
        rule.endMinute = clampMinute(object.optInt("end", 12 * 60));
        rule.target = Target.fromName(object.optString("target", Target.DEEPSEEK.name()));
        rule.skipHolidays = object.optBoolean("skipHolidays", true);
        return rule;
    }

    private static int clampMinute(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 24 * 60 - 1);
    }
}
