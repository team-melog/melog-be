package com.melog.melog.clovaV2.application.port.out;

import com.melog.melog.clovaV2.domain.model.request.SpeechRecognitionRequest;
import com.melog.melog.clovaV2.domain.model.response.SpeechRecognitionResponse;

public interface ClovaV2SpeechPort {
    SpeechRecognitionResponse sendSpeechRecognitionRequest(SpeechRecognitionRequest request);
}
