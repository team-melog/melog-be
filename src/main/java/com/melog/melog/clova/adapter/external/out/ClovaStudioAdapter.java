package com.melog.melog.clova.adapter.external.out;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.ClovaConfig;
import com.melog.melog.clova.domain.model.ClovaEndpoint;
import com.melog.melog.clova.domain.model.ClovaProperties;
import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.ClovaStudioResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaStudioAdapter implements ClovaStudioPort {

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;

    @Override
    public ClovaStudioResponse sendRequest(ClovaStudioRequest request) {
        ClovaEndpoint endpoint = ClovaEndpoint.STUDIO;
        ClovaProperties props = clovaConfig.getProperties(endpoint);
        if (props == null) {
            throw new IllegalArgumentException("No Clova config found for type: " + endpoint);
        }

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.setBearerAuth(props.getApiKey());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + props.getApiKey()); // ← 여기에 진짜 키가 들어가야 함

        System.out.println(headers);
        HttpEntity<Object> entity = new HttpEntity<>(request.getText(), headers);

        ResponseEntity<ClovaStudioResponse> response = restTemplate.exchange(
                props.getUrl(),
                HttpMethod.GET,
                entity,
                ClovaStudioResponse.class);

        System.out.println(response.getBody());

        return response.getBody();
    }
}