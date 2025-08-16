package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.TtsRequest;
import com.melog.melog.clova.domain.model.response.TtsResponse;

public interface TextToSpeakUseCase {

    TtsResponse textToSpeak(TtsRequest request);
    
}
