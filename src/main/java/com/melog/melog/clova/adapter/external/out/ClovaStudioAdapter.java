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
public class ClovaStudioAdapter implements ClovaStudioPort{

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;

    @Override
    public ClovaStudioResponse sendRequest(ClovaStudioRequest request){
        ClovaEndpoint endpoint = ClovaEndpoint.STUDIO;
        ClovaProperties props = clovaConfig.getConfig().get(endpoint);
        if (props == null) {
            throw new IllegalArgumentException("No Clova config found for type: " + endpoint);
        }
        HttpEntity<Object> entity = new HttpEntity<>(request.getText(), new HttpHeaders());

        ResponseEntity<ClovaStudioResponse> response = restTemplate.exchange(
                endpoint.getUrl(),
                HttpMethod.POST, // 이게 ClovaApiRequest에 있으면 좋음. 없으면 HttpMethod.POST로 고정
                entity,
                ClovaStudioResponse.class);

        return response.getBody();
    }
}