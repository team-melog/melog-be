package com.melog.melog.emotion.domain.model.request;

import com.melog.melog.emotion.domain.EmotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionRecordCreateRequest {
    private String text;
    private UserSelectedEmotion userSelectedEmotion;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSelectedEmotion {
        private String type; // 감정 타입 (한글)
        private Integer percentage; // 20 단위로 전달
        
        // 한글 감정명을 EmotionType으로 변환
        public EmotionType getEmotionType() {
            if (type == null) return null;
            for (EmotionType emotionType : EmotionType.values()) {
                if (emotionType.getDescription().equals(type)) {
                    return emotionType;
                }
            }
            throw new IllegalArgumentException("Unknown emotion type: " + type);
        }
    }
} 