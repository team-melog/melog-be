package com.melog.melog.clova.domain.model.request;

import com.melog.melog.clova.domain.model.VoiceType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TtsApiRequest {
    private String text;
    private VoiceType voiceType; 
    private int emotion = 0; // 0 중립, 1 슬픔, 2 기쁨, 3 분노
    private int emotionStrength = 1; // 0 약함, 1 보통, 2 강함
}
