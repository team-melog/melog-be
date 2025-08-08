package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.SpeechSttUseCase;
import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.domain.model.request.ClovaSttRequest;
import com.melog.melog.clova.domain.model.request.SpeechToTextRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;
import com.melog.melog.clova.domain.model.response.SpeechToTextResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeechToTextService implements SpeechSttUseCase {

    private final ClovaSpeechPort clovaSpeechPort;

    @Override
    public SpeechToTextResponse execute(SpeechToTextRequest request) {

        ClovaSttRequest speechRequest = ClovaSttRequest.builder()
                .build();

        ClovaSttResponse response = clovaSpeechPort.sendSpeechToTextRequest(speechRequest);

        return SpeechToTextResponse.builder()
                // .result(response.getText())
                // .confidence(response.getConfidence())
                .build();
    }
}
