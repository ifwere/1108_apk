package com.aichoice.assistant.logic;

import com.aichoice.assistant.data.Target;

/**
 * Result of evaluating the ordered rule list at a specific moment.
 */
public final class Recommendation {

    private final Target target;
    private final String ruleId;
    private final String ruleName;
    private final boolean fallback;

    public Recommendation(Target target, String ruleId, String ruleName, boolean fallback) {
        this.target = target;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.fallback = fallback;
    }

    public Target getTarget() {
        return target;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    /** True when no rule matched and the built-in DeepSeek fallback was used. */
    public boolean isFallback() {
        return fallback;
    }
}
