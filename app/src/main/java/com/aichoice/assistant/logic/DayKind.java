package com.aichoice.assistant.logic;

/**
 * How a calendar day is classified for the built-in "skip statutory holidays"
 * option. Weekends that were turned into make-up workdays are still treated as
 * weekends, matching the requested rule: weekends are always idle time.
 */
public enum DayKind {
    HOLIDAY,
    WEEKEND,
    WORKDAY
}
