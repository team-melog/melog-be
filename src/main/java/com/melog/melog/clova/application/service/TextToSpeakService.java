package com.melog.melog.clova.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.TextToSpeakUseCase;
import com.melog.melog.clova.application.port.out.TextToSpeakPort;
import com.melog.melog.clova.domain.model.VoiceToner;
import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.clova.domain.model.request.TtsApiRequest;
import com.melog.melog.clova.domain.model.request.TtsRequest;
import com.melog.melog.clova.domain.model.response.TtsApiResponse;
import com.melog.melog.clova.domain.model.response.TtsResponse;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest.UserSelectedEmotion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextToSpeakService implements TextToSpeakUseCase {
    private final TextToSpeakPort textToSpeakPort;

    @Override
    public TtsResponse textToSpeak(TtsRequest request) {
        final String text = request.getText();
        final VoiceType voiceType = request.getVoiceType();
        final List<UserSelectedEmotion> emotions = request.getEmotions();
        final VoiceToner toner = VoiceToner.toneFromEmotions(emotions);

        // 서비스 레벨 요약 로그(텍스트는 길 수 있으니 길이만 기록)
        log.info("[TTS] voice={} textLen={} toner={{vol:{}, spd:{}, pit:{}, alp:{}, emo:{}, str:{}}}",
                voiceType.getVoiceKey(),
                (text == null ? 0 : text.length()),
                toner.getVolume(), toner.getSpeed(), toner.getPitch(), toner.getAlpha(),
                toner.getEmotion(), toner.getEmotionStrength());

        TtsApiResponse response = textToSpeakPort.sendTextToSpeakRequest(
                TtsApiRequest.builder()
                        .toner(toner)
                        .voiceType(voiceType)
                        .text(text)
                        .build());

        return TtsResponse.builder()
                .text(text)
                .audioByteArr(response.getAudioByteArr())
                .audioFileSize(response.getAudioFileSize())
                .format(response.getFormat())
                .build();
    }
}