package com.aichoice.assistant.util;

import android.content.Context;

import com.aichoice.assistant.R;
import com.aichoice.assistant.data.Rule;
import com.aichoice.assistant.data.Target;
import com.aichoice.assistant.logic.Recommendation;
import com.aichoice.assistant.logic.RecommendationEngine;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Formats the subtitle on the recommendation cards.
 *
 * <p>When the current recommendation is WorkBuddy it shows how long until the
 * next DeepSeek low-price (idle) window starts. When DeepSeek is already
 * recommended it shows how much of the current low-price window is left. The
 * span keeps days, hours and minutes (seconds are dropped).</p>
 */
public final class LowPriceFormatter {

    private LowPriceFormatter() {
    }

    public static String describe(Context context, RecommendationEngine engine,
                                  List<Rule> rules, ZonedDateTime now,
                                  Recommendation recommendation) {
        boolean idleNow = recommendation.getTarget() == Target.DEEPSEEK;
        ZonedDateTime boundary = idleNow
                ? engine.nextDeepSeekEnd(now, rules)
                : engine.nextDeepSeekStart(now, rules);

        int template = idleNow ? R.string.low_price_current : R.string.low_price_next;
        if (boundary == null) {
            int fallback = idleNow ? R.string.low_price_persistent : R.string.low_price_unavailable;
            return context.getString(template, context.getString(fallback));
        }

        long minutes = Math.max(1,
                (Duration.between(now, boundary).getSeconds() + 59) / 60);
        return context.getString(template, formatSpan(context, minutes));
    }

    static String formatSpan(Context context, long totalMinutes) {
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            if (hours == 0 && minutes == 0) {
                return context.getString(R.string.duration_days, days);
            }
            if (minutes == 0) {
                return context.getString(R.string.duration_days_hours, days, hours);
            }
            return context.getString(R.string.duration_days_hours_minutes, days, hours, minutes);
        }
        if (hours > 0) {
            if (minutes == 0) {
                return context.getString(R.string.duration_hours, hours);
            }
            return context.getString(R.string.duration_hours_minutes, hours, minutes);
        }
        return context.getString(R.string.duration_minutes, minutes);
    }
}
