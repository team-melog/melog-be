package com.melog.melog.emotion.domain.model.response;

import com.melog.melog.user.domain.model.response.UserResponse;
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
    private List<EmotionScoreResponse> emotions; // emotionScores에서 emotions로 변경
    private UserSelectedEmotionResponse userSelectedEmotion;
    private List<EmotionKeywordResponse> emotionKeywords;
} 