package com.melog.melog.emotion.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmotionType {
    JOY("기쁨"),
    EXCITEMENT("설렘"),
    CALMNESS("평온"),
    ANGER("분노"),
    SADNESS("슬픔"),
    GUIDANCE("지침");

    private final String description;

    EmotionType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static EmotionType fromDescription(String description) {
        if (description == null) {
            return CALMNESS; // 기본값
        }
        
        String normalizedDescription = description.trim();
        
        for (EmotionType emotionType : values()) {
            if (emotionType.description.equals(normalizedDescription)) {
                return emotionType;
            }
        }
        
        // 매칭되지 않는 경우 기본값 반환
        return CALMNESS;
    }
} 