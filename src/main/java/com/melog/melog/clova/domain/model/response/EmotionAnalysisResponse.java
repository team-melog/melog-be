package com.melog.melog.clova.domain.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisResponse {
    
    /**
     * 감정 요약 (2-3줄)
     */
    private String summary;
    
    /**
     * 감정 점수 (상위 3개, 백분율)
     */
    private List<EmotionScore> emotions;
    
    /**
     * 키워드 (5개)
     */
    private List<String> keywords;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionScore {
        /**
         * 감정 타입 (한글)
         */
        private String type;
        
        /**
         * 감정 점수 (백분율)
         */
        private Integer percentage;
        
        /**
         * 감정 단계 (1-5)
         * 0-20점: step1, 21-40점: step2, 41-60점: step3, 61-80점: step4, 81-100점: step5
         */
        private Integer step;
        
        /**
         * 점수에 따른 스텝을 자동으로 계산합니다.
         */
        public void calculateStep() {
            if (percentage == null) {
                this.step = 1;
                return;
            }
            
            if (percentage <= 20) {
                this.step = 1;
            } else if (percentage <= 40) {
                this.step = 2;
            } else if (percentage <= 60) {
                this.step = 3;
            } else if (percentage <= 80) {
                this.step = 4;
            } else {
                this.step = 5;
            }
        }
    }
}
