package com.aichoice.assistant.logic;

import java.time.LocalDate;

/**
 * Pure policy for the yearly automatic holiday refresh.
 *
 * <p>The next year's official schedule is usually published in December, so
 * from December 20 onwards the app refreshes the network snapshot once. A
 * successful refresh after that date satisfies the window; a failed attempt is
 * retried on the next day the app is opened.</p>
 */
public final class HolidayAutoRefreshPolicy {

    public static final int REFRESH_MONTH = 12;
    public static final int REFRESH_DAY = 20;

    private HolidayAutoRefreshPolicy() {
    }

    /** First day of the automatic refresh window for the given year. */
    public static LocalDate windowStart(int year) {
        return LocalDate.of(year, REFRESH_MONTH, REFRESH_DAY);
    }

    /** True from December 20 (inclusive) until the end of the year. */
    public static boolean isWindowOpen(LocalDate today) {
        return today != null && !today.isBefore(windowStart(today.getYear()));
    }

    /**
     * @param today       current Beijing date
     * @param lastAttempt date of the last automatic attempt, or null
     * @param lastSuccess date of the last successful refresh (manual or
     *                    automatic), or null
     */
    public static boolean shouldRefresh(LocalDate today,
                                        LocalDate lastAttempt,
                                        LocalDate lastSuccess) {
        if (!isWindowOpen(today)) {
            return false;
        }
        if (lastAttempt != null && !lastAttempt.isBefore(today)) {
            // Already attempted today; do not hammer the network on every
            // launch, retry on a later day instead.
            return false;
        }
        LocalDate start = windowStart(today.getYear());
        return lastSuccess == null || lastSuccess.isBefore(start);
    }
}
