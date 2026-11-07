package com.aichoice.assistant.data;

/**
 * The AI tool that a rule recommends.
 */
public enum Target {
    DEEPSEEK("DeepSeek", "DS"),
    WORKBUDDY("WorkBuddy", "WB");

    private final String displayName;
    private final String shortName;

    Target(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String displayName() {
        return displayName;
    }

    public String shortName() {
        return shortName;
    }

    public static Target fromName(String raw) {
        if (raw != null) {
            for (Target target : values()) {
                if (target.name().equalsIgnoreCase(raw)) {
                    return target;
                }
            }
        }
        return DEEPSEEK;
    }
}
