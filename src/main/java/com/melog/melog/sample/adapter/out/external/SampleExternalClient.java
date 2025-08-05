package com.melog.melog.sample.adapter.out.external;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.melog.melog.sample.application.port.out.ExternalApiPort;
import com.melog.melog.sample.domain.model.request.SampleRestRequest;
import com.melog.melog.sample.domain.model.response.SampleRestResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SampleExternalClient implements ExternalApiPort {

    private final RestTemplate restTemplate;

    private static final String API_URL = "https://external-api.com/generate";

    @Override
    public SampleRestResponse callExternalApi(SampleRestRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("YOUR_API_KEY");

        HttpEntity<SampleRestRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<SampleRestResponse> response = restTemplate.exchange(
                API_URL,
                HttpMethod.POST,
                entity,
                SampleRestResponse.class);

        return response.getBody();
    }
}