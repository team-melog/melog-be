package com.melog.melog.clova.domain.model.request;

import java.util.List;

import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest.UserSelectedEmotion;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TtsRequest {
    private String text;
    private VoiceType voiceType;
    private List<UserSelectedEmotion> emotions;
}
