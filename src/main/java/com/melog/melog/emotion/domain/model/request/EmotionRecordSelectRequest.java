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
            // Jackson이 자동으로 처리하므로 단순화
            return emotionName != null ? EmotionType.fromDescription(emotionName) : EmotionType.CALMNESS;
        }
    }
} 