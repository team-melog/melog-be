package com.melog.melog.clovaV2.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melog.melog.clovaV2.application.port.in.ClovaV2UseCase;
import com.melog.melog.clovaV2.domain.model.request.StudioChatRequest;
import com.melog.melog.clovaV2.domain.model.request.SpeechRecognitionRequest;
import com.melog.melog.clovaV2.domain.model.response.StudioChatResponse;
import com.melog.melog.clovaV2.domain.model.response.SpeechRecognitionResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/clova")
@RequiredArgsConstructor
public class ClovaV2Controller {

    private final ClovaV2UseCase clovaV2UseCase;

    @PostMapping("/studio/chat")
    public StudioChatResponse sendChatRequest(@RequestBody StudioChatRequest request) {
        return clovaV2UseCase.sendChatRequest(request);
    }

    @PostMapping("/speech/recognize")
    public SpeechRecognitionResponse sendSpeechRecognitionRequest(@RequestBody SpeechRecognitionRequest request) {
        return clovaV2UseCase.sendSpeechRecognitionRequest(request);
    }
}
