package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.ClovaSttRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;

public interface ClovaSpeechPort {
    ClovaSttResponse sendSpeechToTextRequest(ClovaSttRequest request);
}
