package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmotionListResponse {
    private List<EmotionRecordSummaryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
} 