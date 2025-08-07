package com.melog.melog.clovaV2.adapter.external.out;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.melog.melog.clovaV2.application.port.out.ClovaV2StudioPort;
import com.melog.melog.clovaV2.domain.model.ClovaV2Config;
import com.melog.melog.clovaV2.domain.model.request.StudioChatRequest;
import com.melog.melog.clovaV2.domain.model.response.StudioChatResponse;
import com.melog.melog.common.util.RestTemplateUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaV2StudioAdapter implements ClovaV2StudioPort {

    private final RestTemplateUtil restTemplateUtil;
    private final ClovaV2Config clovaV2Config;

    @Override
    public StudioChatResponse sendChatRequest(StudioChatRequest request) {
        ClovaV2Config.Studio studioConfig = clovaV2Config.getStudio();
        
        String url = studioConfig.getBaseUrl() + "/v1/chat-completions";
        HttpHeaders headers = createStudioHeaders(studioConfig);

        return restTemplateUtil.sendRequest(
                url,
                HttpMethod.POST,
                request,
                StudioChatResponse.class,
                headers
        );
    }

    /**
     * Studio API 전용 헤더 생성
     */
    private HttpHeaders createStudioHeaders(ClovaV2Config.Studio config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.set("X-NCP-APIGW-API-KEY-ID", config.getApiKeyId());
        return headers;
    }
}
