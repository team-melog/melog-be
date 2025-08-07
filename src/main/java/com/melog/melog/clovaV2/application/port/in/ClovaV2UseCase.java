package com.melog.melog.clovaV2.application.port.in;

import com.melog.melog.clovaV2.domain.model.request.StudioChatRequest;
import com.melog.melog.clovaV2.domain.model.request.SpeechRecognitionRequest;
import com.melog.melog.clovaV2.domain.model.response.StudioChatResponse;
import com.melog.melog.clovaV2.domain.model.response.SpeechRecognitionResponse;

public interface ClovaV2UseCase {
    StudioChatResponse sendChatRequest(StudioChatRequest request);
    SpeechRecognitionResponse sendSpeechRecognitionRequest(SpeechRecognitionRequest request);
}
