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

import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.application.service.AnalyzeSentimentService;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.StudioProps;
import com.melog.melog.clova.domain.model.PromptMessage;
import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.ClovaStudioResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaStudioAdapter implements ClovaStudioPort {

    private static final String CHAT_PATH = "/v1/chat-completions";

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;

    @Override
    public ClovaStudioResponse sendRequest(ClovaStudioRequest request) {
        final StudioProps props = clovaConfig.getStudio();

        final String modelName = props.getModel(); // 예: "HCX-007"
        final String url = props.getBaseUrl() + "/v3/chat-completions/" + modelName;

        final String requestId = UUID.randomUUID().toString();

        // ---- headers (v3) ----
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getApiKey()); // Authorization: Bearer <API_KEY>
        headers.set("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId); // optional
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 스트리밍 원하면 아래 사용
        // headers.setAccept(List.of(MediaType.valueOf("text/event-stream")));

        // ---- payload (v3 Thinking) ----
        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", toV3Messages(request.getPromptMessages()));
        // thinking: effort (low|medium|high) — 기본값 low
        payload.put("thinking", Map.of("effort", "low"));

        // 생성 파라미터 기본값(원하면 ClovaConfig.StudioProps에 추가해서 yml로 조정)
        payload.put("topP", 0.8);
        payload.put("topK", 0);
        payload.put("maxCompletionTokens", 5120);
        payload.put("temperature", 0.5);
        payload.put("repetitionPenalty", 1.1);

        try {
            ResponseEntity<ClovaStudioResponse> res = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), ClovaStudioResponse.class);
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[Clova v3] FAIL rid={} status={} body={}",
                    requestId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("ClovaStudio v3 API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[Clova v3] ERROR rid={}", requestId, e);
            throw new RuntimeException("ClovaStudio v3 API error", e);
        }
    }

    /**
     * v3 messages 변환: content는 배열 또는 문자열 모두 허용됨.
     * 단일 String만 있는 우리 도메인 모델에선 안전하게 "배열 래핑"으로 보냄.
     * (system도 문자열 허용이지만, 배열로 보내도 동작)
     */
    private List<Map<String, Object>> toV3Messages(List<PromptMessage> prompts) {
        if (prompts == null || prompts.isEmpty())
            return List.of();
        return prompts.stream()
                .map(pm -> Map.<String, Object>of(
                        "role", pm.getRole().name().toLowerCase(),
                        "content", List.of(Map.of("type", "text", "text", nullSafe(pm.getContent())))))
                .collect(Collectors.toList());
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

}
