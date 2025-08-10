package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;

public interface EmotionAnalysisUseCase {
    
    /**
     * 텍스트를 기반으로 감정 분석을 수행합니다.
     * 
     * @param request 감정 분석 요청 (텍스트 + 프롬프트)
     * @return 감정 분석 결과 (요약 + 감정 점수)
     */
    EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request);
}
