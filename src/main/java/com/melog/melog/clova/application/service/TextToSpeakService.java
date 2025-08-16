package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.TextToSpeakUseCase;
import com.melog.melog.clova.application.port.out.TextToSpeakPort;
import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.clova.domain.model.request.TtsApiRequest;
import com.melog.melog.clova.domain.model.request.TtsRequest;
import com.melog.melog.clova.domain.model.response.TtsApiResponse;
import com.melog.melog.clova.domain.model.response.TtsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TextToSpeakService implements TextToSpeakUseCase {
    private final TextToSpeakPort textToSpeakPort;

    @Override
    public TtsResponse textToSpeak(TtsRequest request) {

        final String text = request.getText();
        final String nickname = request.getNickname();
        final VoiceType voiceType = request.getVoiceType();

        TtsApiResponse response = textToSpeakPort.sendTextToSpeakRequest(
                TtsApiRequest.builder()
                        .emotion(request.getEmotion())
                        .emotionStrength(request.getEmotionStrength())
                        .voiceType(voiceType)
                        .text(text)
                        .build());

        return TtsResponse.builder()
                .nickname(nickname)
                .text(text)
                .audioByteArr(response.getAudioByteArr())
                .audioFileSize(response.getAudioFileSize())
                .format(response.getFormat())
                .build();
    }

}
