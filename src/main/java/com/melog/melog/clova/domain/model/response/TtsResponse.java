package com.melog.melog.clova.domain.model.response;

import com.melog.melog.clova.domain.model.VoiceType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TtsResponse {
    private String nickname;
    private String text;
    private byte[] audioByteArr;
    private int audioFileSize;
    private String format;
    private VoiceType voiceType;
}
