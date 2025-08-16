package com.melog.melog.clova.adapter.external.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClovaStudioAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ClovaConfig clovaConfig;

    /**
     * Clova Studio API를 호출하여 감정 분석을 수행합니다.
     */
    public EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request) {
        try {
            // Clova Studio API 요청 데이터 구성
            Map<String, Object> requestBody = createClovaStudioRequest(request);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + clovaConfig.getStudio().getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            String url = clovaConfig.getStudio().getBaseUrl() + "/v3/chat-completions/" + clovaConfig.getStudio().getModel();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            // 응답 파싱 및 변환
            return parseClovaStudioResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Clova Studio API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("감정 분석에 실패했습니다.", e);
        }
    }

    /**
     * Clova Studio API를 호출하여 일반 텍스트 생성을 수행합니다.
     */
    public String generateText(String prompt) {
        try {
            // Clova Studio API 요청 데이터 구성
            Map<String, Object> requestBody = createSimpleTextRequest(prompt);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + clovaConfig.getStudio().getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // API 호출
            String url = clovaConfig.getStudio().getBaseUrl() + "/v3/chat-completions/" + clovaConfig.getStudio().getModel();
            log.info("Clova Studio API 호출 - URL: {}, 프롬프트: {}", url, prompt);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("Clova Studio API 응답 상태: {}, 헤더: {}", response.getStatusCode(), response.getHeaders());
            
            // 응답에서 텍스트 추출
            return extractTextFromResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Clova Studio API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("텍스트 생성에 실패했습니다.", e);
        }
    }

    /**
     * 간단한 텍스트 생성을 위한 Clova Studio API 요청 데이터를 구성합니다.
     */
    private Map<String, Object> createSimpleTextRequest(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 300);  // 200자 이내 응답을 위해 300 토큰으로 제한
        requestBody.put("temperature", 0.5);  // 더 일관된 응답을 위해 온도 낮춤
        requestBody.put("top_p", 0.8);
        
        return requestBody;
    }

    /**
     * 응답에서 텍스트를 추출합니다.
     */
    private String extractTextFromResponse(Map responseBody) {
        try {
            log.debug("Clova Studio 응답 구조: {}", responseBody);
            
            // Clova Studio 응답 구조: result.message.content
            Map result = (Map) responseBody.get("result");
            if (result != null) {
                Map message = (Map) result.get("message");
                if (message != null) {
                    log.debug("message: {}", message);
                    
                    String content = (String) message.get("content");
                    if (content != null && !content.trim().isEmpty()) {
                        log.debug("추출된 content: {}", content);
                        return content.trim();
                    } else {
                        log.warn("content가 null이거나 비어있음: {}", content);
                    }
                } else {
                    log.warn("message가 null임");
                }
            } else {
                log.warn("result가 null임");
            }
            
            // OpenAI 호환 구조도 시도: choices[0].message.content
            List<Map> choices = (List<Map>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map choice = choices.get(0);
                log.debug("첫 번째 choice: {}", choice);
                
                Map message = (Map) choice.get("message");
                if (message != null) {
                    log.debug("message: {}", message);
                    
                    String content = (String) message.get("content");
                    if (content != null && !content.trim().isEmpty()) {
                        log.debug("OpenAI 구조에서 추출된 content: {}", content);
                        return content.trim();
                    }
                }
            }
            
            // 응답 구조를 더 자세히 로깅
            log.error("응답 구조 분석 실패 - 전체 응답: {}", responseBody);
            throw new RuntimeException("응답에서 유효한 텍스트를 찾을 수 없습니다. 응답 구조: " + responseBody);
        } catch (Exception e) {
            log.error("텍스트 응답 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("텍스트 응답 파싱에 실패했습니다.", e);
        }
    }

    /**
     * Clova Studio API 요청 데이터를 구성합니다.
     */
    private Map<String, Object> createClovaStudioRequest(EmotionAnalysisRequest request) {
        // 감정 분석을 위한 프롬프트 구성
        String systemPrompt = createSystemPrompt();
        String userPrompt = createUserPrompt(request.getText());

        // Function calling을 위한 tools 정의
        Map<String, Object> tools = Map.of(
            "type", "function",
            "function", Map.of(
                "name", "analyze_emotion",
                "description", "사용자에게 직접 말하듯 1:1 대화 형식으로 감정을 설명·돌아보고·위로/조언까지 제공한 뒤 JSON으로 반환합니다. 제3자 시점, 동화체 서술, 마크다운 금지.",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "summary", Map.of(
                            "type", "string",
                            "description", "반드시 '사용자에게 직접 말하는' 3단계 대화 형식으로 600~800자 작성: "
                                + "① 감정 멘트(1순위 감정 중심, 2~3순위 반영) "
                                + "② 오늘 돌아보기(사건 나열 X, 감정 흐름 중심, 2인칭 구어체) "
                                + "③ 위로와 조언(따뜻하고 현실적인 한마디). "
                                + "제3자/동화체/설명문 금지, 의료·치료 조언 금지."
                        ),
                        "emotions", Map.of(
                            "type", "array",
                            "description", "상위 3개 감정을 백분율로 표시",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "type", Map.of(
                                        "type", "string",
                                        "enum", List.of("기쁨", "설렘", "평온", "분노", "슬픔", "지침"),
                                        "description", "감정 유형"
                                    ),
                                    "percentage", Map.of(
                                        "type", "integer",
                                        "minimum", 0,
                                        "maximum", 100,
                                        "description", "감정 점수 (백분율)"
                                    ),
                                    "step", Map.of(
                                        "type", "integer",
                                        "enum", List.of(1, 2, 3, 4, 5),
                                        "description", "감정 단계 (1: 0-20점, 2: 21-40점, 3: 41-60점, 4: 61-80점, 5: 81-100점)"
                                    )
                                ),
                                "required", List.of("type", "percentage", "step")
                            )
                        ),
                        "keywords", Map.of(
                            "type", "array",
                            "description", "감정 분석 결과에 맞는 키워드 3개 (정의된 키워드 풀에서만 선택)",
                            "items", Map.of("type", "string"),
                            "minItems", 3,
                            "maxItems", 3
                        )
                    ),
                    "required", List.of("summary", "emotions", "keywords")
                )
            )
        );

        // 요청 바디
        Map<String, Object> requestBody = Map.of(
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "tools", List.of(tools),
            // 가능하면 함수 호출을 선호하도록(모델별 스펙 차이 있을 수 있음)
            "toolChoice", "auto",
            // 출력 산만함을 줄이기 위해 온도 낮춤
            "maxTokens", 1024,
            "temperature", 0.2,
            "topP", 0.8
        );

        return requestBody;
    }

    // === [추가] 요약만 재작성(revision) 호출 ===
    public EmotionAnalysisResponse reviseSummaryOnly(String originalText,
                                                     EmotionAnalysisResponse prev,
                                                     int minLen, int maxLen,
                                                     boolean disableFunction) {
        try {
            String system = """
                당신은 감정 분석 전문가이자 따뜻한 상담가입니다.
                지금부터는 '기존 결과를 수정'합니다.
                반드시 사용자에게 직접 말하는 1:1 대화체를 지키고, 제3자/동화체/마크다운/이모지를 금지합니다.
                출력은 JSON 하나만 반환하세요.
                """;

            String user = String.format("""
                [원문 텍스트]
                %s

                [이전 결과(JSON)]
                {
                  "summary": %s,
                  "emotions": %s,
                  "keywords": %s
                }

                [수정 지시]
                1) summary만 다시 작성하세요. 길이 %d~%d자, 3단계(감정 멘트→오늘 돌아보기→위로와 조언), 2인칭 구어체.
                2) emotions와 keywords는 의미적으로 유지하되, JSON 결과에는 그대로 포함하세요.
                3) JSON 외 불필요한 문장 금지.
                """,
                originalText,
                objectMapper.writeValueAsString(prev.getSummary()),
                objectMapper.writeValueAsString(prev.getEmotions()),
                objectMapper.writeValueAsString(prev.getKeywords()),
                minLen, maxLen
            );

            Map<String, Object> requestBody = Map.of(
                "messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)
                ),
                // 리비전에서는 function-calling 비활성화해 "짧게 치는 인자 세팅" 습관을 피함
                "maxTokens", 2048,
                "temperature", 0.2,
                "topP", 0.8
            );

            if (!disableFunction) {
                // 필요 시만 function 사용 (2차 실패 시 3차에서만 on)
                Map<String, Object> tools = Map.of(
                    "type", "function",
                    "function", Map.of(
                        "name", "analyze_emotion",
                        "description", "사용자에게 직접 말하듯 1:1 대화 형식으로 감정을 설명·돌아보고·위로/조언까지 제공한 뒤 JSON으로 반환합니다. 제3자 시점, 동화체 서술, 마크다운 금지.",
                        "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "summary", Map.of(
                                    "type", "string",
                                    "description", "반드시 '사용자에게 직접 말하는' 3단계 대화 형식으로 600~800자 작성: "
                                        + "① 감정 멘트(1순위 감정 중심, 2~3순위 반영) "
                                        + "② 오늘 돌아보기(사건 나열 X, 감정 흐름 중심, 2인칭 구어체) "
                                        + "③ 위로와 조언(따뜻하고 현실적인 한마디). "
                                        + "제3자/동화체/설명문 금지, 의료·치료 조언 금지."
                                ),
                                "emotions", Map.of(
                                    "type", "array",
                                    "description", "상위 3개 감정을 백분율로 표시",
                                    "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                            "type", Map.of(
                                                "type", "string",
                                                "enum", List.of("기쁨", "설렘", "평온", "분노", "슬픔", "지침"),
                                                "description", "감정 유형"
                                            ),
                                            "percentage", Map.of(
                                                "type", "integer",
                                                "minimum", 0,
                                                "maximum", 100,
                                                "description", "감정 점수 (백분율)"
                                            ),
                                            "step", Map.of(
                                                "type", "integer",
                                                "enum", List.of(1, 2, 3, 4, 5),
                                                "description", "감정 단계 (1: 0-20점, 2: 21-40점, 3: 41-60점, 4: 61-80점, 5: 81-100점)"
                                            )
                                        ),
                                        "required", List.of("type", "percentage", "step")
                                    )
                                ),
                                "keywords", Map.of(
                                    "type", "array",
                                    "description", "감정과 연관된 키워드 3개",
                                    "items", Map.of("type", "string"),
                                    "minItems", 3,
                                    "maxItems", 3
                                )
                            ),
                            "required", List.of("summary", "emotions", "keywords")
                        )
                    )
                );
                
                Map<String, Object> newRequestBody = new HashMap<>(requestBody);
                newRequestBody.put("tools", List.of(tools));
                newRequestBody.put("toolChoice", "auto");
                requestBody = newRequestBody;
            }

            // 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + clovaConfig.getStudio().getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = clovaConfig.getStudio().getBaseUrl() + "/v3/chat-completions/" + clovaConfig.getStudio().getModel();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            // 기존 parse 로직 재사용
            EmotionAnalysisResponse r = parseClovaStudioResponse(response.getBody());
            return r;

        } catch (Exception e) {
            log.error("Clova Studio 리비전 호출 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("감정 요약 재작성에 실패했습니다.", e);
        }
    }

    /**
     * 시스템 프롬프트를 생성합니다.
     * 핵심: 1:1 대화형, 제3자 서술 금지, 동화체 금지, JSON만 출력.
     */
    private String createSystemPrompt() {
        return """
            당신은 감정 분석 전문가이자 따뜻한 상담가입니다.
            반드시 '사용자에게 직접 말하는 1:1 대화체'로 피드백을 제공합니다.
            제3자 시점(예: "그/그녀는 ~했다"), 동화책/나레이터식 서술, 마크다운, 이모지 사용을 금지합니다.

            출력은 오직 JSON이어야 합니다. 불필요한 접두/접미 문장, 설명 문구, 코드블록 표시는 금지합니다.

            [요구 형식]
            - summary: 3단계 대화 형식(총 600~800자)
              1) 감정 멘트: 상위 감정(1순위 중심, 2~3순위 자연스럽게 반영)을 기반으로 공감 표현
              2) 오늘 돌아보기: 사건 나열이 아닌 감정의 흐름 중심, 2인칭 구어체(예: "~하셨군요", "~느껴졌네요")
              3) 위로와 조언: 따뜻하고 현실적인 한마디로 마무리
              * 공격적/직설적/판단적 표현 금지, 의료·심리 치료 직접 언급 금지

            - emotions: 기쁨/설렘/평온/분노/슬픔/지침 중 상위 3개를 백분율로, 각 step(1~5) 포함
            - keywords: 아래 키워드 풀에서만 3개 선택
              * 기쁨: "행복", "즐거움", "만족", "신남"
              * 설렘: "기대", "떨림", "긴장", "두근거림"
              * 평온: "차분함", "여유", "안정", "편안함"
              * 분노: "화남", "짜증", "열받음", "격분"
              * 슬픔: "슬픔", "우울함", "절망", "허전함"
              * 지침: "혼란", "막막함", "고민", "갈등"

            [단계(step) 규칙]
            - 0~20: step1, 21~40: step2, 41~60: step3, 61~80: step4, 81~100: step5

            [JSON 예시 구조]
            {
              "summary": "1:1 대화식 감정 피드백(3단계 구조, 400~600자)",
              "emotions": [
                {"type": "기쁨", "percentage": 67, "step": 4},
                {"type": "설렘", "percentage": 22, "step": 2},
                {"type": "평온", "percentage": 11, "step": 1}
              ],
              "keywords": ["키워드1", "키워드2", "키워드3"]
            }
            """;
    }

    /**
     * 사용자 프롬프트를 생성합니다.
     * 핵심: 1:1 대화 시점 고정, 제3자·동화체 금지, JSON만 출력.
     */
    private String createUserPrompt(String text) {
        return String.format("""
            아래 텍스트를 분석해 주세요. 
            반드시 '사용자에게 직접 말하는 1:1 대화체'로 피드백을 작성하고, 제3자/동화체 서술은 금지합니다.
            출력은 오직 JSON 객체만 반환하세요. (코드블록, 주석, 여분 문장 금지)

            [분석 대상 텍스트]
            %s

            [반드시 지킬 것]
            1) summary는 3단계 대화 형식(감정 멘트 → 오늘 돌아보기 → 위로/조언)으로 600~800자 작성
            2) keywords는 시스템 프롬프트에 정의된 키워드 풀에서만 3개 선택
            3) emotions는 상위 3개 감정을 백분율과 step으로 반환
            4) 마크다운/이모지/제3자 서술/동화체/설명문 금지, JSON 외 추가 텍스트 금지
            """, text);
    }


    private static final java.util.Set<String> KEYWORD_POOL = new java.util.HashSet<>(java.util.List.of(
    // 기쁨
"행복","즐거움","만족","신남",
        // 설렘
        "기대","떨림","긴장","두근거림",
        // 평온
        "차분함","여유","안정","편안함",
        // 분노
        "화남","짜증","열받음","격분",
        // 슬픔
        "슬픔","우울함","절망","허전함",
        // 지침
        "혼란","막막함","고민","갈등"
    ));
// 3-2) percentage → step 변환 함수 추가
    private static int computeStep(int p) {
        if (p <= 20) return 1;
        if (p <= 40) return 2;
        if (p <= 60) return 3;
        if (p <= 80) return 4;
        return 5;
    }

    // 3-5) 보조 함수 추가 (클래스 내부 어디든)
    private static void addIfAbsent(List<String> list, String... cands) {
        for (String c : cands) {
            if (list.size() >= 3) break;
            if (KEYWORD_POOL.contains(c) && !list.contains(c)) list.add(c);
        }
    }
    /**
     * Clova Studio API 응답을 파싱하여 EmotionAnalysisResponse로 변환합니다.
     */
    private EmotionAnalysisResponse parseClovaStudioResponse(Map responseBody) {
        
        try {
            // 응답 구조에서 content 추출
            Map result = (Map) responseBody.get("result");
            Map message = (Map) result.get("message");
            
            // Function calling 응답인 경우 toolCalls에서 함수 호출 결과 추출
            List<Map> toolCalls = (List<Map>) message.get("toolCalls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                Map toolCall = toolCalls.get(0);
                Map function = (Map) toolCall.get("function");
                Map arguments = (Map) function.get("arguments");
                
                String summary = (String) arguments.get("summary");
                List<EmotionAnalysisResponse.EmotionScore> emotions = new ArrayList<>();
                List<String> keywords = new ArrayList<>();
                
                // 3-3) 감정 처리 루프 교체 (toolCalls 분기 안의 emotionsList 처리 부분 교체)
                List<Map> emotionsList = (List<Map>) arguments.get("emotions");
                for (Map emotionMap : emotionsList) {
                    Integer pct = (Integer) emotionMap.get("percentage");
                    String type = (String) emotionMap.get("type");

                    if (pct == null) pct = 0;
                    if (type == null || type.isBlank()) type = "평온";

                    EmotionAnalysisResponse.EmotionScore emotionScore = EmotionAnalysisResponse.EmotionScore.builder()
                            .type(type)
                            .percentage(pct)
                            .step(computeStep(pct)) // ✅ 모델 제공 step 무시하고 서버에서 재계산
                            .build();

                    emotions.add(emotionScore);
                }
                
                // 키워드 처리
                List<Object> keywordsList = (List<Object>) arguments.get("keywords");
                if (keywordsList != null) {
                    for (Object kw : keywordsList) {
                        if (kw instanceof String s && KEYWORD_POOL.contains(s)) {
                            keywords.add(s);
                        }
                    }
                }
                // 개수 보정: 3개 미만이면 상위 감정에 맞춰 채워주기
                while (keywords.size() < 3 && !emotions.isEmpty()) {
                    String top = emotions.get(keywords.size() % emotions.size()).getType();
                    switch (top) {
                        case "기쁨" -> addIfAbsent(keywords, "행복", "즐거움", "만족", "신남");
                        case "설렘" -> addIfAbsent(keywords, "기대", "떨림", "긴장", "두근거림");
                        case "평온" -> addIfAbsent(keywords, "차분함", "여유", "안정", "편안함");
                        case "분노" -> addIfAbsent(keywords, "화남", "짜증", "열받음", "격분");
                        case "슬픔" -> addIfAbsent(keywords, "슬픔", "우울함", "절망", "허전함");
                        default     -> addIfAbsent(keywords, "혼란", "막막함", "고민", "갈등");
                    }
                }
                if (keywords.size() > 3) {
                    keywords = keywords.subList(0, 3);
                }
                
                // 로그 출력: 요약과 키워드 확인
                log.info("=== Clova Studio 응답 분석 결과 (Function Calling) ===");
                log.info("요약: {}", summary);
                log.info("키워드: {}", keywords);
                log.info("감정 점수: {}", emotions);
                log.info("===============================================");
                
                return EmotionAnalysisResponse.builder()
                    .summary(summary)
                    .emotions(emotions)
                    .keywords(keywords)
                    .build();
            }
            
            // 일반 텍스트 응답인 경우 (fallback)
            String content = (String) message.get("content");
            if (content != null && !content.trim().isEmpty()) {
                // 마크다운 코드 블록 제거 및 JSON 파싱 시도
                try {
                    // ```json\n...\n``` 형태의 마크다운 코드 블록 제거
                    String cleanContent = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    
                    JsonNode jsonNode = objectMapper.readTree(cleanContent);
                    
                    String summary = jsonNode.path("summary").asText();
                    List<EmotionAnalysisResponse.EmotionScore> emotions = new ArrayList<>();
                    List<String> keywords = new ArrayList<>();
                    
                    // 감정 점수 처리
                    JsonNode emotionsNode = jsonNode.path("emotions");
                    for (JsonNode emotionNode : emotionsNode) {
                        EmotionAnalysisResponse.EmotionScore emotionScore = EmotionAnalysisResponse.EmotionScore.builder()
                            .type(emotionNode.path("type").asText())
                            .percentage(emotionNode.path("percentage").asInt())
                            .step(emotionNode.path("step").asInt(2)) // 기본값 2
                            .build();
                        
                        // 스텝이 없으면 자동으로 계산
                        if (emotionScore.getStep() == null) {
                            emotionScore.calculateStep();
                        }
                        
                        emotions.add(emotionScore);
                    }
                    
                    // 키워드 처리
                    JsonNode keywordsNode = jsonNode.path("keywords");
                    if (keywordsNode.isArray()) {
                        for (JsonNode keywordNode : keywordsNode) {
                            keywords.add(keywordNode.asText());
                        }
                    }
                    
                    // 로그 출력: 요약과 키워드 확인 (JSON 파싱 성공 시)
                    log.info("=== Clova Studio 응답 분석 결과 (JSON 파싱) ===");
                    log.info("요약: {}", summary);
                    log.info("키워드: {}", keywords);
                    log.info("감정 점수: {}", emotions);
                    log.info("===============================================");
                    
                    return EmotionAnalysisResponse.builder()
                        .summary(summary)
                        .emotions(emotions)
                        .keywords(keywords)
                        .build();
                } catch (Exception jsonException) {
                    log.warn("JSON 파싱 실패, 일반 텍스트로 처리: {}", jsonException.getMessage());
                    // 일반 텍스트 응답을 요약으로 사용
                    log.info("=== Clova Studio 일반 텍스트 응답 ===");
                    log.info("요약: {}", content);
                    log.info("=====================================");
                    
                    return EmotionAnalysisResponse.builder()
                        .summary(content)
                        .emotions(new ArrayList<>())
                        .keywords(new ArrayList<>())
                        .build();
                }
            }
            
            throw new RuntimeException("응답에서 유효한 내용을 찾을 수 없습니다.");
                
        } catch (Exception e) {
            log.error("Clova Studio 응답 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("감정 분석 결과 파싱에 실패했습니다.", e);
        }
    }
     
}
