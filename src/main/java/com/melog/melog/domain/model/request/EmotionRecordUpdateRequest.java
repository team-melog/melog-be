package com.melog.melog.domain.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmotionRecordUpdateRequest {
    private String text;
    private String summary;
} 