package com.melog.melog.emotion.domain;

public enum EmotionType {
    JOY("기쁨"),
    ANGER("분노"),
    SADNESS("슬픔"),
    CALM("평온"),
    EXCITEMENT("설렘"),
    CONFUSION("지침");

    private final String description;

    EmotionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 