package com.melog.melog.common.model;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    /** 한글명 → Enum 매핑 */
    @JsonCreator
    public static EmotionType fromDisplayName(String value) {
        return Arrays.stream(values())
                .filter(e -> e.displayName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown emotion type: " + value));
    }
}
