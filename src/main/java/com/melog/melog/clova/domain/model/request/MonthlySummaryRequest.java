package com.melog.melog.clova.domain.model.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MonthlySummaryRequest {
    private List<String> monthlyComments; // 월별 감정 코멘트들 (사용하지 않음)
    private String month; // 월 정보 (예: "2024-01")
    private String prompt; // Clova Studio에 전달할 프롬프트
    private Map<String, Double> emotionDistribution; // 월별 감정 분포 데이터
}
