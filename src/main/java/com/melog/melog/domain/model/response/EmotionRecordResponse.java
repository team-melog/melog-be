package com.melog.melog.domain.model.response;

import com.melog.melog.domain.emotion.EmotionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EmotionRecordResponse {
    private Long id;
    private String text;
    private String summary;
    private LocalDate date;
    private LocalDateTime createdAt;
    private UserResponse user;
    private List<EmotionScoreResponse> emotionScores;
    private UserSelectedEmotionResponse userSelectedEmotion;
    private List<EmotionKeywordResponse> emotionKeywords;
} 