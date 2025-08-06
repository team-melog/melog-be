package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.SpeechSttUseCase;
import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.domain.model.request.ClovaSpeechRequest;
import com.melog.melog.clova.domain.model.request.SpeechSttRequest;
import com.melog.melog.clova.domain.model.response.SpeechSttResponse;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SpeechSttService implements SpeechSttUseCase {

    private final ClovaSpeechPort clovaApiPort;

    @Override
    public SpeechSttResponse execute(SpeechSttRequest request) {
        clovaApiPort.sendRequest(
                ClovaSpeechRequest.builder()
                        .text(request.getText())
                        .build());
        return SpeechSttResponse.builder().result("결과").build();
    }

}
