package com.melog.melog.common.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmotionInsightResponse {
    private List<EmotionKeywordResponse> topKeywords;
    private String monthlySummary;
} 