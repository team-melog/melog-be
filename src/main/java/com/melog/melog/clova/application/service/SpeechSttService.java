package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.SpeechSttUseCase;
import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.domain.model.request.ClovaSpeechRequest;
import com.melog.melog.clova.domain.model.request.SpeechSttRequest;
import com.melog.melog.clova.domain.model.response.ClovaSpeechResponse;
import com.melog.melog.clova.domain.model.response.SpeechSttResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeechSttService implements SpeechSttUseCase {

    private final ClovaSpeechPort clovaSpeechPort;

    @Override
    public SpeechSttResponse execute(SpeechSttRequest request) {
        ClovaSpeechRequest speechRequest = ClovaSpeechRequest.builder()
                .audioData(request.getAudioData())
                .audioFormat(request.getAudioFormat())
                .language(request.getLanguage())
                .diarization(request.getDiarization())
                .build();

        ClovaSpeechResponse response = clovaSpeechPort.sendSpeechRequest(speechRequest);

        return SpeechSttResponse.builder()
                .result(response.getText())
                .confidence(response.getConfidence())
                .build();
    }
}
