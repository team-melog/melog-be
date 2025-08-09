package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.SttRequest;
import com.melog.melog.clova.domain.model.response.SttResponse;

public interface SpeechToTextPort {
    SttResponse sendSpeechToTextRequest(SttRequest request);
}
