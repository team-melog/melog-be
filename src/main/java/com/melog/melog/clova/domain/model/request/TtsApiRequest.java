package com.melog.melog.clova.domain.model.request;

import com.melog.melog.clova.domain.model.VoiceToner;
import com.melog.melog.clova.domain.model.VoiceType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TtsApiRequest {
    private String text;
    private VoiceType voiceType; 
    private VoiceToner toner;
}
