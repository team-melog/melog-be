package com.melog.melog.clova.adapter.external.out;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.out.SpeechToTextPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.SpeechProps;
import com.melog.melog.clova.domain.model.request.SttRequest;
import com.melog.melog.clova.domain.model.response.SttResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeechToTextAdapter implements SpeechToTextPort {
    
    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;
    private final ObjectMapper objectMapper;

    @Override
    public SttResponse sendSpeechToTextRequest(SttRequest request) {
        final SpeechProps props = clovaConfig.getSpeech();
        final String requestId = UUID.randomUUID().toString();

        String url = buildUrl(request, props);
        HttpHeaders headers = buildHeaders(props, requestId);
        
        try {
            // Send raw binary data
            HttpEntity<byte[]> entity = new HttpEntity<>(request.getAudioBinary(), headers);
            
            log.info("[CLOVA STT] REQUEST rid={} url={} headers={} bodySize={}bytes", 
                requestId, url, headers.toSingleValueMap(), request.getAudioBinary().length);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new RuntimeException("CLOVA Speech API returned empty response");
            }
            
            // Parse response according to CLOVA Speech API documentation
            SttResponse sttResponse = parseResponse(root, request);
            
            log.info("[CLOVA STT] SUCCESS rid={} text={}", requestId, sttResponse.getText());

            return sttResponse;

        } catch (HttpStatusCodeException e) {
            log.error("[CLOVA STT] FAIL rid={} status={} body={}", 
                    requestId, e.getStatusCode(), e.getResponseBodyAsString());
            throw createClovaException(e);
        } catch (Exception e) {
            log.error("[CLOVA STT] ERROR rid={}", requestId, e);
            throw new RuntimeException("CLOVA Speech API error", e);
        }
    }

    private String buildUrl(SttRequest request, SpeechProps props) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(props.getUrl() + props.getStt().getEndpoint());
        
        // Required parameters
        String language = request.getLanguage() != null ? request.getLanguage() : props.getStt().getDefaultLang();
        builder.queryParam("lang", language);
        
        // Optional parameters
        if (request.getAssessment() != null) {
            builder.queryParam("assessment", request.getAssessment());
        }
        
        if (request.getUtterance() != null) {
            builder.queryParam("utterance", request.getUtterance());
        }
        
        if (request.getBoostings() != null) {
            builder.queryParam("boostings", request.getBoostings());
        }
        
        if (request.getGraph() != null) {
            builder.queryParam("graph", request.getGraph());
        }
        
        return builder.toUriString();
    }

    private HttpHeaders buildHeaders(SpeechProps props, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", props.getClientId());
        headers.set("X-NCP-APIGW-API-KEY", props.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return headers;
    }

    private SttResponse parseResponse(JsonNode root, SttRequest request) {
        String text = root.path("text").asText("");
        Integer quota = root.path("quota").asInt(0);
        Integer assessmentScore = root.path("assessment_score").asInt(0);
        String assessmentDetails = root.path("assessment_details").asText("");
        
        // Parse graph arrays
        List<Integer> refGraph = parseGraphArray(root.path("ref_graph"));
        List<Integer> usrGraph = parseGraphArray(root.path("usr_graph"));
        
        return SttResponse.builder()
                .text(text)
                .quota(quota)
                .assessmentScore(assessmentScore)
                .assessmentDetails(assessmentDetails)
                .refGraph(refGraph)
                .usrGraph(usrGraph)
                .language(request.getLanguage())
                .build();
    }
    
    private List<Integer> parseGraphArray(JsonNode graphNode) {
        if (graphNode.isArray()) {
            List<Integer> graph = new ArrayList<>();
            for (JsonNode node : graphNode) {
                graph.add(node.asInt(0));
            }
            return graph;
        }
        return new ArrayList<>();
    }

    private RuntimeException createClovaException(HttpStatusCodeException e) {
        String statusCode = e.getStatusCode().toString();
        String responseBody = e.getResponseBodyAsString();
        
        // Handle 401 Unauthorized specifically
        if ("401".equals(statusCode)) {
            return new RuntimeException("CLOVA Speech API Authentication Failed (401): Check X-NCP-APIGW-API-KEY-ID and X-NCP-APIGW-API-KEY headers. Response: " + responseBody);
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
                case "STT998", "STT999" -> new RuntimeException("CLOVA Speech internal server error: " + errorMessage);
                default -> new RuntimeException("CLOVA Speech API error: " + statusCode + " - " + (errorMessage.isEmpty() ? responseBody : errorMessage));
            };
        } catch (Exception parseError) {
            return new RuntimeException("CLOVA Speech API error: " + statusCode + " - " + responseBody);
        }
    }
}
