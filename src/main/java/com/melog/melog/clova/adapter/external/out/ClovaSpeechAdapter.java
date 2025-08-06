package com.melog.melog.clova.adapter.external.out;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.domain.model.ClovaConfig;
import com.melog.melog.clova.domain.model.ClovaEndpoint;
import com.melog.melog.clova.domain.model.ClovaProperties;
import com.melog.melog.clova.domain.model.request.ClovaSpeechRequest;
import com.melog.melog.clova.domain.model.response.ClovaSpeechResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaSpeechAdapter implements ClovaSpeechPort{

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;

    @Override
    public ClovaSpeechResponse sendRequest(ClovaSpeechRequest request){
        ClovaEndpoint endpoint = ClovaEndpoint.SPEECH_STT;
        ClovaProperties props = clovaConfig.getProperties(endpoint);
        if (props == null) {
            throw new IllegalArgumentException("No Clova config found for type: " + endpoint);
        }
        HttpEntity<Object> entity = new HttpEntity<>(request.getText(), new HttpHeaders());

        ResponseEntity<ClovaSpeechResponse> response = restTemplate.exchange(
                endpoint.getUrl(),
                HttpMethod.POST, // 이게 ClovaApiRequest에 있으면 좋음. 없으면 HttpMethod.POST로 고정
                entity,
                ClovaSpeechResponse.class);

        return response.getBody();
    }
}