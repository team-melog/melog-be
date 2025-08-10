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
         * 감정 단계 (1-3)
         */
        private Integer step;
    }
}
