package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.TtsApiRequest;
import com.melog.melog.clova.domain.model.response.TtsApiResponse;

public interface TextToSpeakPort {
    TtsApiResponse sendTextToSpeakRequest(TtsApiRequest request);
}
