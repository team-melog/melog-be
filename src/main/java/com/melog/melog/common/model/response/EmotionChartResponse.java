package com.melog.melog.common.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class EmotionChartResponse {
    private Map<String, Integer> thisMonth;
    private Map<String, Integer> compareWithLastMonth;
} 