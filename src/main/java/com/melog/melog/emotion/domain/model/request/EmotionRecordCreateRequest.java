package com.melog.melog.emotion.domain.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmotionRecordCreateRequest {
    private String text;
    private UserSelectedEmotion userSelectedEmotion;
    
    @Getter
    @NoArgsConstructor
    public static class UserSelectedEmotion {
        private String type; // 감정 타입 (한글)
        private Integer percentage; // 20 단위로 전달
    }
} 