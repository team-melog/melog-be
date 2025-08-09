package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EmotionRecordSummaryResponse {
    private Long id;
    private LocalDate date;
    private String summary;
    private List<EmotionScoreResponse> emotions;
} 