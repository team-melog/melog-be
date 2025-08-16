package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class EmotionChartResponse {
    private Map<String, Double> thisMonth;
    private Map<String, Double> compareWithLastMonth;
} 