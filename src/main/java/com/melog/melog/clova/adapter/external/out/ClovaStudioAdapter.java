package com.melog.melog.clova.adapter.external.out;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.ClovaEndpoint;
import com.melog.melog.clova.domain.model.request.ClovaStudioChatRequest;
import com.melog.melog.clova.domain.model.response.ClovaStudioChatResponse;
import com.melog.melog.common.util.RestTemplateUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaStudioAdapter implements ClovaStudioPort {

    private final ClovaApiAdapter clovaApiAdapter;

    @Override
    public ClovaStudioChatResponse sendChatRequest(ClovaStudioChatRequest request) {
        return clovaApiAdapter.sendRequest(
                ClovaEndpoint.STUDIO_CHAT,
                request,
                ClovaStudioChatResponse.class
        );
    }

    /**
     * 텍스트 생성 API 호출
     */
    public ClovaStudioChatResponse sendTextGenerationRequest(ClovaStudioChatRequest request) {
        return clovaApiAdapter.sendRequest(
                ClovaEndpoint.STUDIO_TEXT,
                request,
                ClovaStudioChatResponse.class
        );
    }

    /**
     * 특정 모델 정보 조회 (Path Variable 사용)
     */
    public ClovaStudioChatResponse getModelInfo(String modelId) {
        Object[] pathVariables = {modelId};
        
        return clovaApiAdapter.sendRequestWithPathVariables(
                ClovaEndpoint.STUDIO_MODEL_INFO,
                pathVariables,
                null,
                ClovaStudioChatResponse.class
        );
    }
}