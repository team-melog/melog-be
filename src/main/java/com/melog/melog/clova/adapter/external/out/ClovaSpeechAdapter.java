package com.melog.melog.clova.adapter.external.out;

import org.springframework.stereotype.Component;

import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.domain.model.ClovaEndpoint;
import com.melog.melog.clova.domain.model.request.ClovaSpeechRequest;
import com.melog.melog.clova.domain.model.response.ClovaSpeechResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaSpeechAdapter implements ClovaSpeechPort {

    private final ClovaApiAdapter clovaApiAdapter;

    @Override
    public ClovaSpeechResponse sendSpeechRequest(ClovaSpeechRequest request) {
        return clovaApiAdapter.sendRequest(
                ClovaEndpoint.SPEECH_STT,
                request,
                ClovaSpeechResponse.class
        );
    }

    /**
     * 음성 합성 API 호출
     */
    public ClovaSpeechResponse sendTextToSpeechRequest(ClovaSpeechRequest request) {
        return clovaApiAdapter.sendRequest(
                ClovaEndpoint.SPEECH_TTS,
                request,
                ClovaSpeechResponse.class
        );
    }
}