package com.melog.melog.emotion.domain.model.request;

import com.melog.melog.emotion.domain.EmotionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EmotionRecordSelectRequest {
    private List<EmotionSelection> emotions;
    
    @Getter
    @NoArgsConstructor
    public static class EmotionSelection {
        private EmotionType type;
        private Integer percentage;
    }
} 