package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmotionKeywordResponse {
    private String keyword;
    private Integer weight; // 키워드 중요도 (1-100)
} 