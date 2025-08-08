package com.melog.melog.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmotionListResponse {
    private List<EmotionRecordSummaryResponse> content;
    private int page;
    private int size;
} 