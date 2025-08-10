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
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmotionResult {
        @JsonIgnore
        private EmotionType emotion;
        private int percentage;
        
        // API 응답에서 한글 감정명을 사용하기 위한 getter
        @JsonProperty("type")
        public String getType() {
            return emotion != null ? emotion.getDescription() : null;
        }
    }
}