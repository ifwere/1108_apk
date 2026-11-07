package com.aichoice.assistant.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.aichoice.assistant.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists the ordered rule list and countdown duration in SharedPreferences.
 * The order in the stored array is the evaluation priority.
 */
public final class RuleRepository {

    private static final String TAG = "RuleRepository";
    private static final String PREFS_NAME = "ai_choice_prefs";
    private static final String KEY_RULES = "rules_json_v1";
    private static final String KEY_COUNTDOWN = "countdown_seconds";

    public static final int DEFAULT_COUNTDOWN_SECONDS = 5;
    public static final int MIN_COUNTDOWN_SECONDS = 1;
    public static final int MAX_COUNTDOWN_SECONDS = 30;

    private final Context appContext;
    private final SharedPreferences preferences;

    public RuleRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<Rule> getRules() {
        String raw = preferences.getString(KEY_RULES, null);
        if (raw == null) {
            List<Rule> defaults = createDefaultRules();
            saveRules(defaults);
            return defaults;
        }
        try {
            return parseRules(raw);
        } catch (JSONException e) {
            Log.e(TAG, "Stored rules are corrupted, restoring defaults", e);
            List<Rule> defaults = createDefaultRules();
            saveRules(defaults);
            return defaults;
        }
    }

    public synchronized void saveRules(List<Rule> rules) {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            try {
                array.put(rule.toJson());
            } catch (JSONException e) {
                Log.e(TAG, "Unable to serialize rule " + rule.getName(), e);
            }
        }
        preferences.edit().putString(KEY_RULES, array.toString()).apply();
    }

    public synchronized Rule findById(String id) {
        if (id == null) {
            return null;
        }
        for (Rule rule : getRules()) {
            if (id.equals(rule.getId())) {
                return rule;
            }
        }
        return null;
    }

    public synchronized void add(Rule rule) {
        List<Rule> rules = getRules();
        rules.add(rule);
        saveRules(rules);
    }

    public synchronized void update(Rule updated) {
        if (updated == null) {
            return;
        }
        List<Rule> rules = getRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(updated.getId())) {
                rules.set(i, updated);
                saveRules(rules);
                return;
            }
        }
        rules.add(updated);
        saveRules(rules);
    }

    public synchronized void delete(String id) {
        List<Rule> rules = getRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(id)) {
                rules.remove(i);
                saveRules(rules);
                return;
            }
        }
    }

    /** Moves a rule one position up ({@code -1}) or down ({@code +1}). */
    public synchronized void move(String id, int delta) {
        List<Rule> rules = getRules();
        int index = indexOf(rules, id);
        if (index < 0) {
            return;
        }
        int targetIndex = Math.max(0, Math.min(rules.size() - 1, index + delta));
        if (targetIndex == index) {
            return;
        }
        Rule rule = rules.remove(index);
        rules.add(targetIndex, rule);
        saveRules(rules);
    }

    public synchronized void resetToDefaults() {
        saveRules(createDefaultRules());
    }

    public int getCountdownSeconds() {
        return clampCountdown(preferences.getInt(KEY_COUNTDOWN, DEFAULT_COUNTDOWN_SECONDS));
    }

    public void setCountdownSeconds(int seconds) {
        preferences.edit().putInt(KEY_COUNTDOWN, clampCountdown(seconds)).apply();
    }

    public List<Rule> createDefaultRules() {
        List<Rule> rules = new ArrayList<>();

        Rule morning = new Rule();
        morning.setName(appContext.getString(R.string.default_rule_morning));
        morning.setDays(weekdays());
        morning.setStartMinute(9 * 60);
        morning.setEndMinute(12 * 60);
        morning.setTarget(Target.WORKBUDDY);
        morning.setSkipHolidays(true);
        rules.add(morning);

        Rule afternoon = new Rule();
        afternoon.setName(appContext.getString(R.string.default_rule_afternoon));
        afternoon.setDays(weekdays());
        afternoon.setStartMinute(14 * 60);
        afternoon.setEndMinute(18 * 60);
        afternoon.setTarget(Target.WORKBUDDY);
        afternoon.setSkipHolidays(true);
        rules.add(afternoon);

        Rule idle = new Rule();
        idle.setName(appContext.getString(R.string.default_rule_idle));
        idle.setDays(allDays());
        idle.setStartMinute(0);
        idle.setEndMinute(0);
        idle.setTarget(Target.DEEPSEEK);
        idle.setSkipHolidays(false);
        rules.add(idle);

        return rules;
    }

    private static List<Rule> parseRules(String raw) throws JSONException {
        JSONArray array = new JSONArray(raw);
        List<Rule> rules = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object != null) {
                rules.add(Rule.fromJson(object));
            }
        }
        return rules;
    }

    private static int indexOf(List<Rule> rules, String id) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean[] weekdays() {
        return new boolean[]{true, true, true, true, true, false, false};
    }

    private static boolean[] allDays() {
        return new boolean[]{true, true, true, true, true, true, true};
    }

    private static int clampCountdown(int seconds) {
        return Math.max(MIN_COUNTDOWN_SECONDS, Math.min(MAX_COUNTDOWN_SECONDS, seconds));
    }
}
