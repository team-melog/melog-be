package com.melog.melog.clovaV2.adapter.external.out;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.melog.melog.clovaV2.application.port.out.ClovaV2SpeechPort;
import com.melog.melog.clovaV2.domain.model.ClovaV2Config;
import com.melog.melog.clovaV2.domain.model.request.SpeechRecognitionRequest;
import com.melog.melog.clovaV2.domain.model.response.SpeechRecognitionResponse;
import com.melog.melog.common.util.RestTemplateUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaV2SpeechAdapter implements ClovaV2SpeechPort {

    private final RestTemplateUtil restTemplateUtil;
    private final ClovaV2Config clovaV2Config;

    @Override
    public SpeechRecognitionResponse sendSpeechRecognitionRequest(SpeechRecognitionRequest request) {
        ClovaV2Config.Speech speechConfig = clovaV2Config.getSpeech();
        
        String url = speechConfig.getBaseUrl() + "/v1/recognize";
        HttpHeaders headers = createSpeechHeaders(speechConfig);

        return restTemplateUtil.sendRequest(
                url,
                HttpMethod.POST,
                request,
                SpeechRecognitionResponse.class,
                headers
        );
    }

    /**
     * Speech API 전용 헤더 생성
     */
    private HttpHeaders createSpeechHeaders(ClovaV2Config.Speech config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.set("X-NCP-APIGW-API-KEY-ID", config.getApiKeyId());
        return headers;
    }
}
