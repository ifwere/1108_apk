package com.aichoice.assistant.data;

import android.content.Context;

import com.aichoice.assistant.logic.HolidayCalendar;
import com.aichoice.assistant.logic.RecommendationEngine;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Single entry point for building the holiday calendar from all sources.
 * Precedence: custom dates > network snapshot > bundled asset > weekday rule.
 */
public final class HolidayDataProvider {

    private static final DateTimeFormatter FETCH_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US);

    private HolidayDataProvider() {
    }

    /** Bundled asset with any cached network snapshot merged over it. */
    public static HolidayCalendar baseCalendar(Context context) {
        HolidayCalendar bundled = HolidayCalendar.fromAssets(context);
        HolidayCalendar remote = remoteCalendar(context);
        if (remote == null) {
            return bundled;
        }
        return remote.mergeOver(bundled);
    }

    /** Full calendar including user-defined holidays. */
    public static HolidayCalendar load(Context context) {
        Map<LocalDate, String> custom = new CustomHolidayRepository(context).getAll();
        return baseCalendar(context).withAdditionalHolidays(custom);
    }

    /**
     * Parses and caches a freshly downloaded snapshot. The cache is only
     * written after parsing succeeds, so a broken response never destroys the
     * last good data.
     */
    public static HolidayCalendar refreshFromNetwork(Context context, String rawJson)
            throws Exception {
        HolidayCalendar remote = HolidayCalendar.fromChineseDaysJson(rawJson);
        if (remote.maxYear() == 0) {
            throw new IllegalArgumentException("network data contains no dates");
        }
        String fetchedAt = FETCH_TIME_FORMAT.format(
                ZonedDateTime.now(RecommendationEngine.BEIJING_ZONE));
        new HolidayCacheRepository(context).save(rawJson, fetchedAt);
        return load(context);
    }

    /** Cached snapshot, or null when absent or unreadable. */
    public static HolidayCalendar remoteCalendar(Context context) {
        HolidayCacheRepository cache = new HolidayCacheRepository(context);
        String raw = cache.getRawJson();
        if (raw == null) {
            return null;
        }
        try {
            HolidayCalendar remote = HolidayCalendar.fromChineseDaysJson(raw);
            if (remote.maxYear() == 0) {
                return null;
            }
            String fetchedAt = cache.getFetchedAt();
            return fetchedAt.isEmpty() ? remote : remote.withUpdated(fetchedAt);
        } catch (Exception ignored) {
            return null;
        }
    }
}
