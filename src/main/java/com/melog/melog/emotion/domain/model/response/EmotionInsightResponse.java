package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmotionInsightResponse {
    private List<EmotionKeywordResponse> topKeywords; // 최대 5개 키워드 (weight 포함)
    private String monthlySummary; // 3줄 요약
} 