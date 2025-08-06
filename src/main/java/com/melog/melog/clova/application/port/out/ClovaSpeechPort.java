package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.ClovaSpeechRequest;
import com.melog.melog.clova.domain.model.response.ClovaSpeechResponse;

public interface ClovaSpeechPort {
    ClovaSpeechResponse sendRequest(ClovaSpeechRequest request);
}
