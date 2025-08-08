package com.melog.melog.common.model;
import java.util.Arrays;

public enum EmotionType {
    JOY("기쁨"),
    SADNESS("슬픔"),
    ANGER("분노"),
    ANXIETY("불안"),
    EXHAUSTION("지침"),
    EXCITEMENT("설렘");

    private final String displayName;

    EmotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EmotionType fromNameIgnoreCase(String name) {
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid emotion type: " + name));
    }
}