package com.melog.melog.clova.domain.model.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisRequest {
    
    /**
     * 분석할 텍스트 내용
     */
    private String text;
    
    /**
     * 감정 분석을 위한 프롬프트
     */
    private String prompt;
}
