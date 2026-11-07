package com.aichoice.assistant.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class RuleJsonTest {

    @Test
    public void roundTripPreservesEveryField() throws Exception {
        Rule rule = new Rule();
        rule.setName("工作日高峰");
        rule.setEnabled(false);
        rule.setDays(new boolean[]{true, true, true, true, true, false, false});
        rule.setStartMinute(9 * 60);
        rule.setEndMinute(12 * 60);
        rule.setTarget(Target.WORKBUDDY);
        rule.setSkipHolidays(true);

        Rule restored = Rule.fromJson(new JSONObject(rule.toJson().toString()));

        assertEquals(rule.getId(), restored.getId());
        assertEquals("工作日高峰", restored.getName());
        assertFalse(restored.isEnabled());
        assertArrayEquals(rule.getDays(), restored.getDays());
        assertEquals(540, restored.getStartMinute());
        assertEquals(720, restored.getEndMinute());
        assertEquals(Target.WORKBUDDY, restored.getTarget());
        assertTrue(restored.isSkipHolidays());
    }

    @Test
    public void missingFieldsUseSafeDefaults() throws Exception {
        Rule restored = Rule.fromJson(new JSONObject("{}"));

        assertFalse(restored.getId().isEmpty());
        assertEquals("", restored.getName());
        assertTrue(restored.isEnabled());
        assertTrue(restored.isDaySelected(0));
        assertTrue(restored.isDaySelected(4));
        assertFalse(restored.isDaySelected(5));
        assertEquals(Target.DEEPSEEK, restored.getTarget());
        assertTrue(restored.isSkipHolidays());
    }

    @Test
    public void crossedMidnightRangeIsAccepted() {
        Rule rule = new Rule();
        rule.setStartMinute(22 * 60);
        rule.setEndMinute(6 * 60);

        assertEquals(22 * 60, rule.getStartMinute());
        assertEquals(6 * 60, rule.getEndMinute());
        assertFalse(rule.isAllDay());
    }

    @Test
    public void equalTimesMeanAllDay() {
        Rule rule = new Rule();
        rule.setStartMinute(0);
        rule.setEndMinute(0);

        assertTrue(rule.isAllDay());
    }

    @Test
    public void minutesAreClampedToValidRange() {
        Rule rule = new Rule();
        rule.setStartMinute(-10);
        rule.setEndMinute(5000);

        assertEquals(0, rule.getStartMinute());
        assertEquals(23 * 60 + 59, rule.getEndMinute());
    }

    @Test
    public void copyIsIndependent() {
        Rule original = new Rule();
        original.setName("原始");
        original.setDay(0, true);

        Rule copy = original.copy();
        copy.setName("副本");
        copy.setDay(0, false);

        assertEquals("原始", original.getName());
        assertTrue(original.isDaySelected(0));
        assertEquals("副本", copy.getName());
        assertFalse(copy.isDaySelected(0));
    }
}
