package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EmotionCalendarResponse {
    private Long id; // 상세 페이지 연결용
    private LocalDate date;
    private List<EmotionScoreResponse> emotions;
} 