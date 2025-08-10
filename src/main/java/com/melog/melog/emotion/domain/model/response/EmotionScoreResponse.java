package com.melog.melog.emotion.domain.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.melog.melog.emotion.domain.EmotionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmotionScoreResponse {
    private Long id;
    @JsonIgnore
    private EmotionType emotionType;
    private Integer percentage;
    private Integer step;
    
    // API 응답에서 한글 감정명을 사용하기 위한 getter
    @JsonProperty("type")
    public String getType() {
        return emotionType != null ? emotionType.getDescription() : null;
    }
} 