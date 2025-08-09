package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EmotionCalendarResponse {
    private LocalDate date;
    private List<EmotionScoreResponse> emotions;
} 