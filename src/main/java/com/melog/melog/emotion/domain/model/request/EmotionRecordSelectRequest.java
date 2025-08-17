package com.melog.melog.emotion.domain.model.request;

import com.melog.melog.emotion.domain.EmotionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EmotionRecordSelectRequest {
    private List<EmotionSelection> emotions;
    
    @Getter
    @NoArgsConstructor
    public static class EmotionSelection {
        private EmotionType type;
        private Integer percentage;
        
        // 기본 생성자
        public EmotionSelection(EmotionType type, Integer percentage) {
            this.type = type;
            this.percentage = percentage;
        }
        
        // 한글 감정명으로부터 EmotionType을 찾는 생성자
        public EmotionSelection(String emotionName, Integer percentage) {
            this.type = findEmotionTypeByName(emotionName);
            this.percentage = percentage;
        }
        
        private EmotionType findEmotionTypeByName(String emotionName) {
            if (emotionName == null) return null;
            
            String normalizedName = emotionName.trim();
            
            // 정확한 매칭 시도
            for (EmotionType emotionType : EmotionType.values()) {
                if (emotionType.getDescription().equals(normalizedName)) {
                    return emotionType;
                }
            }
            
            // 기본값 반환 (에러 대신)
            return EmotionType.CALMNESS;
        }
    }
} 