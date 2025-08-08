package com.melog.melog.clova.adapter.external.out;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.config.ClovaConfig.SpeechProps;
import com.melog.melog.clova.domain.model.request.ClovaSttRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;

import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class ClovaSpeechAdapter implements ClovaSpeechPort {
    private final RestTemplate restTemplate;
    // private final SpeechProps speechProps;

    @Override
    public ClovaSttResponse sendSpeechToTextRequest(ClovaSttRequest request) {


        return ClovaSttResponse.builder()
                .text("Sample Transcribed Text") // Placeholder for actual transcription result
                .confidence(0.95) // Placeholder for confidence score
                .build();
    }

}
