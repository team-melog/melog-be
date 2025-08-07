package com.melog.melog.domain.model.request;

import com.melog.melog.domain.emotion.EmotionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class EmotionRecordCreateRequest {
    private String text;
    private String summary;
    private LocalDate date;
    private EmotionType selectedEmotion;
    private Integer selectedEmotionPercentage;
    private Integer selectedEmotionStep;
} 