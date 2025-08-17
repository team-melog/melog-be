package com.melog.melog.clova.domain.model.response;

import com.melog.melog.clova.domain.model.VoiceType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TtsApiResponse {
    private byte[] audioByteArr;
    private VoiceType voiceType;
    private String format;
    private int audioFileSize;
}
