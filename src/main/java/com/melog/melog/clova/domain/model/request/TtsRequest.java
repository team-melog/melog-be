package com.melog.melog.clova.domain.model.request;

import com.melog.melog.clova.domain.model.VoiceType;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TtsRequest {
    private String nickname;
    private String text;
    private VoiceType voiceType;
    private int emotion;
    private int emotionStrength;
}
