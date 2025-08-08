package com.melog.melog.clova.adapter.external.out;

import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.out.ClovaSpeechPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.SpeechProps;
import com.melog.melog.clova.domain.model.request.ClovaSttRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaSpeechAdapter implements ClovaSpeechPort {
    
    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;
    private final ObjectMapper objectMapper;

    @Override
    public ClovaSttResponse sendSpeechToTextRequest(ClovaSttRequest request) {
        final SpeechProps props = clovaConfig.getSpeech();
        final String requestId = UUID.randomUUID().toString();

        String url = buildUrl(request);
        HttpHeaders headers = buildHeaders(props, requestId);
        
        try {
            // Send raw binary data (not base64 encoded)
            HttpEntity<byte[]> entity = new HttpEntity<>(request.getAudioBinary(), headers);
            
            log.info("[CSR] REQUEST rid={} url={} headers={} bodySize={}bytes", 
                requestId, url, headers.toSingleValueMap(), request.getAudioBinary().length);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new RuntimeException("CSR API returned empty response");
            }
            
            // Parse response according to official CSR API documentation
            String recognizedText = root.path("return_text").asText();
            if (recognizedText.isEmpty()) {
                // Fallback to check other possible field names
                recognizedText = root.path("text").asText();
            }

            log.info("[CSR] SUCCESS rid={} text={}", requestId, recognizedText);

            return ClovaSttResponse.builder()
                    .text(recognizedText)
                    .confidence(null) // CSR API doesn't provide confidence in response
                    .language(request.getLanguage())
                    .build();

        } catch (HttpStatusCodeException e) {
            log.error("[CSR] FAIL rid={} status={} body={}", 
                    requestId, e.getStatusCode(), e.getResponseBodyAsString());
            throw createCsrException(e);
        } catch (Exception e) {
            log.error("[CSR] ERROR rid={}", requestId, e);
            throw new RuntimeException("CSR API error", e);
        }
    }

    private String buildUrl(ClovaSttRequest request) {
        String language = mapLanguage(request.getLanguage());
        return "https://naveropenapi.apigw.ntruss.com/recog/v1/stt?lang=" + language;
    }

    private HttpHeaders buildHeaders(SpeechProps props, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", props.getApiKeyId());
        headers.set("X-NCP-APIGW-API-KEY", props.getApiKey());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        System.out.println(headers);
        return headers;
    }

    private String mapLanguage(String language) {
        if (language == null) return "Kor";
        
        return switch (language.toLowerCase()) {
            case "ko", "korean" -> "Kor";
            case "en", "english" -> "Eng";
            case "ja", "japanese" -> "Jpn";
            case "zh", "chinese" -> "Chn";
            default -> "Kor";
        };
    }

    private RuntimeException createCsrException(HttpStatusCodeException e) {
        String statusCode = e.getStatusCode().toString();
        String responseBody = e.getResponseBodyAsString();
        
        // Handle 401 Unauthorized specifically
        if ("401".equals(statusCode)) {
            return new RuntimeException("CSR API Authentication Failed (401): Check X-NCP-APIGW-API-KEY-ID and X-NCP-APIGW-API-KEY headers. Response: " + responseBody);
        }
        
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody);
            String errorCode = errorNode.path("errorCode").asText();
            String errorMessage = errorNode.path("errorMessage").asText();

            return switch (errorCode) {
                case "STT000" -> new RuntimeException("File size exceeded (3MB limit): " + errorMessage);
                case "STT001" -> new RuntimeException("Audio duration exceeded (60 seconds limit): " + errorMessage);
                case "STT002", "STT003", "STT004" -> new RuntimeException("Missing or invalid headers/body: " + errorMessage);
                case "STT005" -> new RuntimeException("Invalid language parameter: " + errorMessage);
                case "STT998", "STT999" -> new RuntimeException("CSR internal server error: " + errorMessage);
                default -> new RuntimeException("CSR API error: " + statusCode + " - " + (errorMessage.isEmpty() ? responseBody : errorMessage));
            };
        } catch (Exception parseError) {
            return new RuntimeException("CSR API error: " + statusCode + " - " + responseBody);
        }
    }
}
