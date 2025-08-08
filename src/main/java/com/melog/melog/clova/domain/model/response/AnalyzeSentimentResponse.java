package com.melog.melog.clova.domain.model.response;

import java.util.List;

import com.melog.melog.clova.domain.model.response.ClovaStudioResponse.EmotionResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyzeSentimentResponse {
    
    private List<EmotionResult> emotionResults;

}
