package com.melog.melog.clova.domain.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.melog.melog.emotion.domain.EmotionType;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractEmotionResponse {
    private List<EmotionResult> emotionResults;
    private String summary;
    private List<String> keywords;

    @Getter
    @AllArgsConstructor
    public static class EmotionResult {
        private EmotionType emotion;
        private int percentage;
    }
}