package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmotionInsightResponse {
    private List<EmotionKeywordResponse> topKeywords; // 상위 키워드 3개 (weight 포함)
    private String monthlyComment; // 한줄 조언 텍스트
} 