package com.melog.melog.clova.domain.model;

import java.util.function.BiConsumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClovaEndpoint {
    STUDIO(
        "/studio", 
        HttpMethod.POST,
        (headers, props) -> headers.set("Authorization", "Bearer " + props.getApiKey())),
    SPEECH_STT(
        "/speech", 
        HttpMethod.POST,
        (headers, props) -> headers.set("Authorization", "Bearer " + props.getApiKey()));

    private final String url;
    private final HttpMethod method;
    private final BiConsumer<HttpHeaders, ClovaProperties> authStrategy;
}
