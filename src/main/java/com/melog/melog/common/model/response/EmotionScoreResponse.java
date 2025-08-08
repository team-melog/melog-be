package com.melog.melog.common.model.response;

import com.melog.melog.domain.emotion.EmotionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmotionScoreResponse {
    private Long id;
    private EmotionType emotionType;
    private Integer percentage;
    private Integer step;
} 