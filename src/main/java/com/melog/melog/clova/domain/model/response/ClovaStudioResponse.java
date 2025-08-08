package com.melog.melog.clova.domain.model.response;

import java.util.List;

import com.melog.melog.common.model.EmotionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class ClovaStudioResponse {

    List<EmotionResult> emotionResults;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmotionResult {
        private EmotionType emotion;
        private double percentage;
    }

}
