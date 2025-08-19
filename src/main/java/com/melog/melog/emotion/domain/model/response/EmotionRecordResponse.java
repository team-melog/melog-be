package com.melog.melog.emotion.domain.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
public class EmotionRecordResponse {
    private Long id;
    private String text;
    private String summary;
    private String comment;
    private LocalDate date;
    private LocalDateTime createdAt;
    private List<EmotionScoreResponse> emotions; // 상위 3개 감정만 포함
    private UserSelectedEmotionResponse userSelectedEmotion;
    private String audioFilePath;
    private Boolean hasAudioFile;
} 