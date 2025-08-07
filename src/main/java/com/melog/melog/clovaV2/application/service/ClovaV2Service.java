package com.melog.melog.clovaV2.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clovaV2.application.port.in.ClovaV2UseCase;
import com.melog.melog.clovaV2.application.port.out.ClovaV2SpeechPort;
import com.melog.melog.clovaV2.application.port.out.ClovaV2StudioPort;
import com.melog.melog.clovaV2.domain.model.request.StudioChatRequest;
import com.melog.melog.clovaV2.domain.model.request.SpeechRecognitionRequest;
import com.melog.melog.clovaV2.domain.model.response.StudioChatResponse;
import com.melog.melog.clovaV2.domain.model.response.SpeechRecognitionResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClovaV2Service implements ClovaV2UseCase {

    private final ClovaV2StudioPort studioPort;
    private final ClovaV2SpeechPort speechPort;

    @Override
    public StudioChatResponse sendChatRequest(StudioChatRequest request) {
        return studioPort.sendChatRequest(request);
    }

    @Override
    public SpeechRecognitionResponse sendSpeechRecognitionRequest(SpeechRecognitionRequest request) {
        return speechPort.sendSpeechRecognitionRequest(request);
    }
}
