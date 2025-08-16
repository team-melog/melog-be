package com.melog.melog.clova.adapter.external.out;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.out.ExtractEmotionPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.StudioProps;
import com.melog.melog.clova.domain.model.PromptMessage;
import com.melog.melog.clova.domain.model.request.ExtractEmotionRequest;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse.EmotionResult;
import com.melog.melog.emotion.domain.EmotionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractEmotionAdapter implements ExtractEmotionPort {

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;
    private final ObjectMapper objectMapper;

    @Override
    public ExtractEmotionResponse sendRequest(ExtractEmotionRequest request) {
        StudioProps props = clovaConfig.getStudio();
        String requestId = UUID.randomUUID().toString();

        HttpHeaders headers = buildHeaders(props, requestId);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildPayload(request), headers);

        String url = buildUrl(props);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class);

            JsonNode root = response.getBody();
            JsonNode messageNode = root.path("result").path("message");
            String contentJson = messageNode.path("content").asText();

            // content는 JSON 문자열이므로 다시 파싱
            JsonNode contentNode = objectMapper.readTree(contentJson);
            JsonNode emotionNode = contentNode.path("emotionResults");

            List<Map<String, Object>> rawEmotions = objectMapper.readValue(
                emotionNode.traverse(),
                new TypeReference<>() {}
        );

            // Map을 EmotionResult로 변환하면서 emotionType 매핑
            List<EmotionResult> parsed = rawEmotions.stream()
                    .map(raw -> {
                        // 'type' 또는 'emotion' 키 모두 허용
                        String typeKo = null;
                        if (raw.get("type") instanceof String) typeKo = (String) raw.get("type");
                        else if (raw.get("emotion") instanceof String) typeKo = (String) raw.get("emotion");

                        if (typeKo == null || typeKo.isBlank()) {
                            log.warn("감정 타입 누락 → 기본값 '평온' 처리");
                            typeKo = "평온";
                        }

                        int percentage;
                        Object pctObj = raw.get("percentage");
                        if (pctObj instanceof Integer) percentage = (Integer) pctObj;
                        else if (pctObj instanceof Number) percentage = ((Number) pctObj).intValue();
                        else {
                            log.warn("percentage 비정상 값: {} → 0으로 대체", pctObj);
                            percentage = 0;
                        }

                        EmotionType emotionType = mapEmotionType(typeKo);
                        return EmotionResult.builder()
                                .emotion(emotionType)
                                .percentage(percentage)
                                .build();
                    })
                    .collect(Collectors.toList());


            // 로그 출력: 감정 분석 결과 확인
            log.info("=== ExtractEmotion 응답 분석 결과 ===");
            log.info("감정 결과: {}", parsed);
            log.info("=====================================");
            
            return ExtractEmotionResponse.builder()
                    .emotionResults(parsed)
                    .build();

        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Clova Studio API 요청 실패: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Clova Studio 응답 파싱 실패", e);
        }
    }

    private String buildUrl(StudioProps props) {
        return props.getBaseUrl() + "/v3/chat-completions/" + props.getModel();
    }

    private HttpHeaders buildHeaders(StudioProps props, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getApiKey());
        headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // === 교체 1: buildPayload 전체 대체 ===
    private Map<String, Object> buildPayload(ExtractEmotionRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", toV3Messages(request.getPromptMessages()));
        payload.put("thinking", Map.of("effort", "low"));
        payload.put("topP", 0.8);
        payload.put("topK", 0);
        payload.put("maxCompletionTokens", 5120); // 충분히 크게
        payload.put("temperature", 0.2);         // 톤 흔들림 최소화
        payload.put("repetitionPenalty", 1.1);
        return payload;
    }

    private List<Map<String, Object>> toV3Messages(List<PromptMessage> prompts) {
        return prompts.stream()
            .map(pm -> Map.of(
                "role", pm.getRole().name().toLowerCase(),
                "content", List.of(Map.of("type", "text", "text", nullSafe(pm.getContent())))
            ))
            .collect(Collectors.toList());
    }
    

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * 한글 감정명을 EmotionType enum으로 매핑
     */
    private EmotionType mapEmotionType(String emotionTypeStr) {
        if (emotionTypeStr == null) {
            return EmotionType.CALMNESS; // 기본값
        }
        
        return switch (emotionTypeStr.trim()) {
            case "기쁨" -> EmotionType.JOY;
            case "설렘" -> EmotionType.EXCITEMENT;
            case "평온" -> EmotionType.CALMNESS;
            case "분노" -> EmotionType.ANGER;
            case "슬픔" -> EmotionType.SADNESS;
            case "지침" -> EmotionType.GUIDANCE;
            default -> {
                log.warn("알 수 없는 감정 타입: {}, 기본값 CALMNESS 사용", emotionTypeStr);
                yield EmotionType.CALMNESS;
            }
        };
    }
}