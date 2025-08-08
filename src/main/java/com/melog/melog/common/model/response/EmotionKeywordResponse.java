package com.melog.melog.common.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmotionKeywordResponse {
    private Long id;
    private String keyword;
    private Integer weight;
} 