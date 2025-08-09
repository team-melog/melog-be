package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.ExtractEmotionRequest;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse;

public interface ExtractEmotionPort {
    ExtractEmotionResponse sendRequest(ExtractEmotionRequest request);
}
