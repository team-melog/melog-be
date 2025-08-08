package com.melog.melog.common.model.response;

import com.melog.melog.emotion.domain.EmotionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSelectedEmotionResponse {
    private Long id;
    private EmotionType emotionType;
    private Integer percentage;
    private Integer step;
} 