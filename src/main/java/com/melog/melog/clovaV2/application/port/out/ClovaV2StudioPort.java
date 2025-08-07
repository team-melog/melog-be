package com.melog.melog.clovaV2.application.port.out;

import com.melog.melog.clovaV2.domain.model.request.StudioChatRequest;
import com.melog.melog.clovaV2.domain.model.response.StudioChatResponse;

public interface ClovaV2StudioPort {
    StudioChatResponse sendChatRequest(StudioChatRequest request);
}
