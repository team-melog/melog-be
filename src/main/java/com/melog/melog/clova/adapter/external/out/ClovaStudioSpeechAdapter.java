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
import com.melog.melog.clova.application.port.out.ClovaStudioSpeechPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.StudioSpeechProps;
import com.melog.melog.clova.domain.model.request.ClovaSttRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaStudioSpeechAdapter implements ClovaStudioSpeechPort {

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;
    private final ObjectMapper objectMapper;

    @Override
    public ClovaSttResponse sendShortSentenceSttRequest(ClovaSttRequest request) {
        final StudioSpeechProps props = clovaConfig.getStudioSpeech();
        final String requestId = UUID.randomUUID().toString();

        String url = buildUrl(props);
        HttpHeaders headers = buildHeaders(props, requestId);
        
        try {
            // Send raw binary data for Clova Studio Speech API
            HttpEntity<byte[]> entity = new HttpEntity<>(request.getAudioBinary(), headers);
            
            log.info("[Clova Studio Short Sentence] REQUEST rid={} url={} bodySize={}bytes", 
                requestId, url, request.getAudioBinary().length);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new RuntimeException("Clova Studio Short Sentence API returned empty response");
            }
            
            // Parse response according to Clova Studio Short Sentence API documentation
            String recognizedText = extractRecognizedText(root);

            log.info("[Clova Studio Short Sentence] SUCCESS rid={} text={}", requestId, recognizedText);

            return ClovaSttResponse.builder()
                    .text(recognizedText)
                    .confidence(extractConfidence(root))
                    .language(request.getLanguage())
                    .build();

        } catch (HttpStatusCodeException e) {
            log.error("[Clova Studio Short Sentence] FAIL rid={} status={} body={}", 
                    requestId, e.getStatusCode(), e.getResponseBodyAsString());
            throw createStudioSpeechException(e);
        } catch (Exception e) {
            log.error("[Clova Studio Short Sentence] ERROR rid={}", requestId, e);
            throw new RuntimeException("Clova Studio Short Sentence API error", e);
        }
    }

    private String buildUrl(StudioSpeechProps props) {
        return props.getBaseUrl();
    }

    private HttpHeaders buildHeaders(StudioSpeechProps props, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-CLOVASPEECH-API-KEY", props.getApiKey());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return headers;
    }

    private String extractRecognizedText(JsonNode root) {
        // Parse response according to Clova Studio Short Sentence API documentation
        // Expected response format: {"text": "recognized text", "confidence": 0.95}
        String text = root.path("text").asText();
        if (!text.isEmpty()) return text;
        
        // Fallback: Check for results array format
        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) {
            JsonNode firstResult = results.get(0);
            text = firstResult.path("text").asText();
            if (!text.isEmpty()) return text;
        }
        
        // Additional fallbacks for various response formats
        text = root.path("result").path("text").asText();
        if (!text.isEmpty()) return text;
        
        text = root.path("return_text").asText();
        if (!text.isEmpty()) return text;
        
        return "";
    }
    
    private Double extractConfidence(JsonNode root) {
        // Extract confidence according to Clova Studio Short Sentence API format
        JsonNode confidenceNode = root.path("confidence");
        if (!confidenceNode.isMissingNode()) {
            return confidenceNode.asDouble();
        }
        
        // Fallback: Check in results array
        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) {
            JsonNode firstResult = results.get(0);
            confidenceNode = firstResult.path("confidence");
            if (!confidenceNode.isMissingNode()) {
                return confidenceNode.asDouble();
            }
        }
        
        return null;
    }

    private RuntimeException createStudioSpeechException(HttpStatusCodeException e) {
        String statusCode = e.getStatusCode().toString();
        String responseBody = e.getResponseBodyAsString();
        
        // Handle 401 Unauthorized specifically
        if ("401".equals(statusCode)) {
            return new RuntimeException("Clova Studio Short Sentence API Authentication Failed (401): Check X-NCP-CLOVASPEECH-API-KEY header. Response: " + responseBody);
        }
        
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody);
            String errorCode = errorNode.path("errorCode").asText();
            String errorMessage = errorNode.path("errorMessage").asText();

            return switch (errorCode) {
                case "A001" -> new RuntimeException("Invalid API key: " + errorMessage);
                case "A002" -> new RuntimeException("Request quota exceeded: " + errorMessage);
                case "A003" -> new RuntimeException("Service temporarily unavailable: " + errorMessage);
                case "B001" -> new RuntimeException("Invalid audio format: " + errorMessage);
                case "B002" -> new RuntimeException("Audio file too large: " + errorMessage);
                case "B003" -> new RuntimeException("Audio duration too long (max 10 seconds): " + errorMessage);
                case "B004" -> new RuntimeException("Audio data corrupted: " + errorMessage);
                case "C001" -> new RuntimeException("Speech recognition failed: " + errorMessage);
                case "C002" -> new RuntimeException("No speech detected in audio: " + errorMessage);
                default -> new RuntimeException("Clova Studio Short Sentence API error: " + statusCode + " - " + (errorMessage.isEmpty() ? responseBody : errorMessage));
            };
        } catch (Exception parseError) {
            return new RuntimeException("Clova Studio Short Sentence API error: " + statusCode + " - " + responseBody);
        }
    }
}