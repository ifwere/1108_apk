package com.aichoice.assistant.logic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

public class HolidayAutoRefreshPolicyTest {

    @Test
    public void windowOpensOnDecember20() {
        assertFalse(HolidayAutoRefreshPolicy.isWindowOpen(LocalDate.of(2026, 12, 19)));
        assertTrue(HolidayAutoRefreshPolicy.isWindowOpen(LocalDate.of(2026, 12, 20)));
        assertTrue(HolidayAutoRefreshPolicy.isWindowOpen(LocalDate.of(2026, 12, 31)));
    }

    @Test
    public void windowIsClosedOutsideDecember() {
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 11, 30), null, null));
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2027, 1, 1), null, null));
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 3, 1), null, LocalDate.of(2025, 12, 20)));
    }

    @Test
    public void refreshIsDueWhenNothingWasFetchedThisWindow() {
        assertTrue(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 12, 20), null, null));
        assertTrue(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 12, 25), null, LocalDate.of(2025, 12, 21)));
    }

    @Test
    public void successInsideWindowSuppressesRefresh() {
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 12, 20), null, LocalDate.of(2026, 12, 20)));
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 12, 28), null, LocalDate.of(2026, 12, 21)));
    }

    @Test
    public void failedAttemptIsRetriedOnTheNextDay() {
        LocalDate today = LocalDate.of(2026, 12, 20);
        assertFalse(HolidayAutoRefreshPolicy.shouldRefresh(today, today, null));
        assertTrue(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2026, 12, 21), today, null));
    }

    @Test
    public void previousYearsSuccessDoesNotCoverTheNewWindow() {
        assertTrue(HolidayAutoRefreshPolicy.shouldRefresh(
                LocalDate.of(2027, 12, 20), null, LocalDate.of(2026, 12, 20)));
    }
}
