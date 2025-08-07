package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.ClovaStudioChatRequest;
import com.melog.melog.clova.domain.model.response.ClovaStudioChatResponse;

public interface ClovaStudioPort {
    ClovaStudioChatResponse sendChatRequest(ClovaStudioChatRequest request);
    ClovaStudioChatResponse sendTextGenerationRequest(ClovaStudioChatRequest request);
    ClovaStudioChatResponse getModelInfo(String modelId);
}
