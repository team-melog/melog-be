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
                "description", "텍스트의 감정을 분석하고 요약하는 함수",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "summary", Map.of(
                            "type", "string",
                            "description", "텍스트 내용을 2-3줄로 간결하게 요약"
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
                            "description", "텍스트에서 추출한 핵심 키워드 5개",
                            "items", Map.of("type", "string"),
                            "minItems", 5,
                            "maxItems", 5
                        )
                    ),
                    "required", List.of("summary", "emotions", "keywords")
                )
            )
        );
        
        Map<String, Object> requestBody = Map.of(
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "tools", List.of(tools),
            "toolChoice", "auto",
            "maxTokens", 1024,
            "temperature", 0.3,
            "topP", 0.8
        );
        
        return requestBody;
    }

    /**
     * 시스템 프롬프트를 생성합니다.
     */
    private String createSystemPrompt() {
        return """
            당신은 감정 분석 전문가입니다. 주어진 텍스트를 분석하여 다음 세 가지를 제공해야 합니다:
            
            1. 감정 요약: 텍스트의 내용을 2-3줄로 간결하게 요약하여 핵심 감정과 상황을 명확하게 표현
            2. 감정 점수: 다음 6가지 감정에 대해 100점 만점으로 점수를 매기고, 상위 3개를 백분율로 표시
               - 기쁨: 긍정적이고 즐거운 감정
               - 설렘: 기대와 긴장이 섞인 감정
               - 평온: 차분하고 안정적인 감정
               - 분노: 화나고 격분한 감정
               - 슬픔: 우울하고 슬픈 감정
               - 지침: 막막하고 불안한 감정
            3. 키워드 추출: 텍스트에서 가장 중요한 핵심 키워드 5개를 추출
            
            감정 점수에 따른 단계:
            - 0-20점: step1 (매우 약함)
            - 21-40점: step2 (약함)
            - 41-60점: step3 (보통)
            - 61-80점: step4 (강함)
            - 81-100점: step5 (매우 강함)
            
            analyze_emotion 함수를 사용하여 구조화된 형태로 응답해주세요.
            """;
    }

    /**
     * 사용자 프롬프트를 생성합니다.
     */
    private String createUserPrompt(String text) {
        return String.format("""
            다음 텍스트를 분석해주세요:
            
            %s
            
            위 텍스트에 대한 감정 요약, 감정 점수, 키워드를 analyze_emotion 함수를 사용하여 응답해주세요.
            """, text);
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
                
                // 감정 점수 처리
                List<Map> emotionsList = (List<Map>) arguments.get("emotions");
                for (Map emotionMap : emotionsList) {
                    EmotionAnalysisResponse.EmotionScore emotionScore = EmotionAnalysisResponse.EmotionScore.builder()
                        .type((String) emotionMap.get("type"))
                        .percentage((Integer) emotionMap.get("percentage"))
                        .step((Integer) emotionMap.get("step"))
                        .build();
                    
                    // 스텝이 없으면 자동으로 계산
                    if (emotionScore.getStep() == null) {
                        emotionScore.calculateStep();
                    }
                    
                    emotions.add(emotionScore);
                }
                
                // 키워드 처리
                List<Object> keywordsList = (List<Object>) arguments.get("keywords");
                if (keywordsList != null) {
                    for (Object keyword : keywordsList) {
                        if (keyword instanceof String) {
                            keywords.add((String) keyword);
                        }
                    }
                }
                
                return EmotionAnalysisResponse.builder()
                    .summary(summary)
                    .emotions(emotions)
                    .keywords(keywords)
                    .build();
            }
            
            // 일반 텍스트 응답인 경우 (fallback)
            String content = (String) message.get("content");
            if (content != null && !content.trim().isEmpty()) {
                // JSON 파싱 시도
                try {
                    JsonNode jsonNode = objectMapper.readTree(content);
                    
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
                    
                    return EmotionAnalysisResponse.builder()
                        .summary(summary)
                        .emotions(emotions)
                        .keywords(keywords)
                        .build();
                } catch (Exception jsonException) {
                    log.warn("JSON 파싱 실패, 일반 텍스트로 처리: {}", jsonException.getMessage());
                    // 일반 텍스트 응답을 요약으로 사용
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
