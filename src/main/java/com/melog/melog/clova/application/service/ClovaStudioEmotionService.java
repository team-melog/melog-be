package com.melog.melog.clova.application.service;

import com.melog.melog.clova.application.port.in.EmotionAnalysisUseCase;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;
import com.melog.melog.clova.adapter.external.out.ClovaStudioAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClovaStudioEmotionService implements EmotionAnalysisUseCase {

    private final ClovaStudioAdapter clovaStudioAdapter;

    @Override
    public EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request) {
        log.info("Clova Studio 감정 분석 시작: 텍스트 길이 = {}", request.getText().length());
        
        try {
            // Clova Studio API 호출
            EmotionAnalysisResponse response = clovaStudioAdapter.analyzeEmotion(request);
            
            log.info("Clova Studio 감정 분석 완료: 요약 길이 = {}, 감정 개수 = {}", 
                    response.getSummary().length(), response.getEmotions().size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Clova Studio 감정 분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("감정 분석에 실패했습니다.", e);
        }
    }
}
