package com.melog.melog.clova.domain.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.melog.melog.common.model.EmotionType;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClovaStudioResponse {
    private List<EmotionResult> emotionResults;

    @Getter
    @AllArgsConstructor
    public static class EmotionResult {
        private EmotionType emotion;
        private int percentage;
    }
}