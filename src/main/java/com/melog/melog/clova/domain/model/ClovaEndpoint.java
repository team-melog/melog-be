package com.melog.melog.clova.domain.model;

import java.util.function.BiConsumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClovaEndpoint {
    // Clova Studio (대화형 AI)
    STUDIO_CHAT(
        "/v1/chat-completions", 
        HttpMethod.POST,
        (headers, props) -> {
            headers.set("Authorization", "Bearer " + props.getApiKey());
            headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        }),
    
    // Clova Studio (텍스트 생성)
    STUDIO_TEXT(
        "/v1/text-generations", 
        HttpMethod.POST,
        (headers, props) -> {
            headers.set("Authorization", "Bearer " + props.getApiKey());
            headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        }),
    
    // Clova Speech Recognition (CSR)
    SPEECH_STT(
        "/v1/recognize", 
        HttpMethod.POST,
        (headers, props) -> {
            headers.set("Authorization", "Bearer " + props.getApiKey());
            headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        }),
    
    // Clova Speech Synthesis (TTS)
    SPEECH_TTS(
        "/v1/tts", 
        HttpMethod.POST,
        (headers, props) -> {
            headers.set("Authorization", "Bearer " + props.getApiKey());
            headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        }),
    
    // Path Variable이 있는 예시: 특정 모델 정보 조회
    STUDIO_MODEL_INFO(
        "/v1/models/{modelId}", 
        HttpMethod.GET,
        (headers, props) -> {
            headers.set("Authorization", "Bearer " + props.getApiKey());
            headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        });

    private final String url;
    private final HttpMethod method;
    private final BiConsumer<HttpHeaders, ClovaProperties> authStrategy;
}
