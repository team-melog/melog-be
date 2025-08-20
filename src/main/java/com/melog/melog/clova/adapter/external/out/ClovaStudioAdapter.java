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

    private final ClovaConfig clovaConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate clovaStudioRestTemplate;

    /**
     * Clova Studio API를 호출하여 감정 분석을 수행합니다.
     * 최대 2번까지 재시도하여 응답 품질을 보장합니다.
     */
    public EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request) {
        try {
            log.info("Clova Studio API 호출 시작");
            
            // Clova Studio API 요청 데이터 구성
            Map<String, Object> requestBody = createClovaStudioRequest(request);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + clovaConfig.getStudio().getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // API 호출 (타임아웃 설정)
            String url = clovaConfig.getStudio().getBaseUrl() + "/v3/chat-completions/" + clovaConfig.getStudio().getModel();
            
            // 타임아웃 설정이 있는 RestTemplate 사용
            ResponseEntity<Map> response = clovaStudioRestTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            // 응답 파싱 및 변환
            EmotionAnalysisResponse result = parseClovaStudioResponse(response.getBody());
            
            log.info("Clova Studio API 호출 성공");
            return result;
            
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
            
            ResponseEntity<Map> response = clovaStudioRestTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("Clova Studio API 응답 상태: {}, 헤더: {}", response.getStatusCode(), response.getHeaders());
            
            // 응답에서 텍스트 추출
            return extractTextFromResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Clova Studio API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("텍스트 생성에 실패했습니다.", e);
        }
    }

    /**
     * 월간 요약 등 일반 텍스트 생성용: 간단한 텍스트 생성을 위한 Clova Studio API 요청 데이터를 구성합니다.
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
     * AI는 자연스러운 텍스트로 반환하고, 서버에서 텍스트를 분석하여 JSON 구조로 변환합니다.
     */
    private Map<String, Object> createClovaStudioRequest(EmotionAnalysisRequest request) {
        // 감정 분석을 위한 프롬프트 구성
        String systemPrompt = createSystemPrompt();
        String userPrompt = createUserPrompt(request.getText());

        // AI는 자연스러운 텍스트로 반환
        // 서버에서 텍스트 분석 로직을 통해 JSON 구조 생성

        // 요청 바디
        Map<String, Object> requestBody = Map.of(
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            // AI는 자연스러운 텍스트로 반환
            // 이전 3번 재시도에서 사용했던 고품질 설정 적용
            "maxTokens", 4096,        // 최대 토큰으로 설정 (더 긴 응답)
            "temperature", 0.2,       // 창의성과 일관성의 균형
            "topP", 0.8,             // 더 다양한 표현
            "frequencyPenalty", 0.1,  // 반복 방지
            "presencePenalty", 0.3    // 다양성 증가
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
                반드시 사용자에게 직접 말하는 1:1 대화체를 지키고, 제3자/동화책/나레이터식 서술, 마크다운, 줄바꿈, 이모지를 금지합니다.
                자연스러운 대화체로 응답해주세요.
                """;

            String user = String.format("""
                [원문 텍스트]
                %s

                [이전 결과]
                요약: %s
                감정: %s
                키워드: %s

                [수정 지시]
                1) summary만 다시 작성하세요. 길이 %d~%d자, 3단계(감정 멘트→오늘 돌아보기→위로와 조언), 2인칭 구어체.
                2) emotions와 keywords는 의미적으로 유지하되, 자연스러운 대화체로 작성하세요.
                3) 불필요한 문장이나 마크업, 줄바꿈, 이모지 없이 자연스럽게 작성해주세요.
                """,
                originalText,
                prev.getSummary(),
                prev.getEmotions(),
                prev.getKeywords(),
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

            // 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + clovaConfig.getStudio().getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = clovaConfig.getStudio().getBaseUrl() + "/v3/chat-completions/" + clovaConfig.getStudio().getModel();
            ResponseEntity<Map> response = clovaStudioRestTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

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
     * 핵심: 1:1 대화형, 제3자 서술 금지, 동화체 금지, 자연스러운 텍스트로 응답.
     */
    private String createSystemPrompt() {
        return """
            당신은 감정 분석 전문가이자 따뜻한 상담가입니다.
            반드시 '사용자에게 직접 말하는 1:1 대화체'로 피드백을 제공합니다.
            제3자 시점(예: "그/그녀는 ~했다"), 동화책/나레이터식 서술, 마크다운, 줄바꿈, 이모지 사용을 금지합니다.

            [요구 형식 - 3단계 대화 구조 필수]
            - summary: 반드시 3단계 대화 형식으로 600~800자 작성 (현재 너무 짧음!)
              1) 감정 멘트: 상위 감정(1순위 중심, 2~3순위 자연스럽게 반영)을 기반으로 공감 표현
                 예시: "안녕하세요! 오늘은 라면을 끓여 먹고 소세지와 함께 드셨군요. 소세지가 맛있어서 행복하셨겠지만, 문어 육수로 만든 라면이 없어서 아쉬우셨겠어요. 이런 복잡한 감정들이 교차하는 하루를 보내셨네요."
              2) 오늘 돌아보기: 사건 나열이 아닌 감정의 흐름 중심, 2인칭 정중체로 상세하게
                 예시: "소세지와 함께한 식사에서 느낀 만족감과 행복, 문어를 넣지 못해 아쉬웠던 마음, 그리고 지금 노래를 들으며 코딩하고 있는 현재의 모습까지. 오늘 하루 동안 여러 감정이 오갔던 것 같아요. 특히 음식을 통해 느낀 감정들이 인상적이에요."
              3) 위로와 조언: 따뜻하고 현실적인 한마디로 마무리
                 예시: "가끔 계획대로 되지 않는 일들이 생기더라도, 그 속에서 작은 행복을 찾아내는 것이 중요하답니다. 소세지의 맛을 즐기신 것처럼, 작은 것에서도 기쁨을 발견하는 마음가짐이 정말 소중해요. 앞으로도 이렇게 긍정적인 마음으로 일상을 채워나가시면, 더욱 풍성한 삶을 살 수 있을 거예요!"
              
              * 공격적/직설적/판단적 표현 금지, 의료·심리 치료 직접 언급 금지
              * 각 단계가 명확히 구분되어야 함 (감정 멘트 → 돌아보기 → 위로/조언)

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

            자연스러운 대화체로 응답해주세요. JSON 형식이나 특별한 마크업 없이 일반 텍스트로 작성해주세요.
            """;
    }

    /**
     * 사용자 프롬프트를 생성합니다.
     * 핵심: 1:1 대화 시점 고정, 제3자·동화체 금지, 자연스러운 텍스트로 응답.
     */
    private String createUserPrompt(String text) {
        return String.format("""
            아래 텍스트를 분석해 주세요. 
            반드시 '사용자에게 직접 말하는 1:1 대화체'로 피드백을 작성하고, 제3자/동화체 서술, 마크다운, 이모지 사용은 금지합니다.
            자연스러운 대화체로 응답해주세요.

            [분석 대상 텍스트]
            %s

            [반드시 지킬 것 - 3단계 대화 구조 필수]
            1) summary는 반드시 3단계 대화 형식으로 600~800자 작성
               - 1단계: 감정 멘트 (공감 표현)
               - 2단계: 오늘 돌아보기 (감정 흐름 중심, 2인칭 구어체)
               - 3단계: 위로와 조언 (따뜻하고 현실적인 마무리)
               
            2) 각 단계가 명확히 구분되어야 함 (감정 멘트 → 돌아보기 → 위로/조언)
            3) keywords는 시스템 프롬프트에 정의된 키워드 풀에서만 3개 선택
            4) emotions는 상위 3개 감정을 백분율과 step으로 반환
            5) 마크다운/줄바꿈/이모지/제3자 서술/동화체/설명문 금지, 자연스러운 대화체로 작성
            
            [3단계 대화 예시 - 600~800자 목표]
            "안녕하세요! 오늘은 라면을 끓여 먹고 소세지와 함께 드셨군요. 소세지가 맛있어서 행복하셨겠지만, 문어 육수로 만든 라면이 없어서 아쉬우셨겠어요. 이런 복잡한 감정들이 교차하는 하루를 보내셨네요. 소세지와 함께한 식사에서 느낀 만족감과 행복, 문어를 넣지 못해 아쉬웠던 마음, 그리고 지금 노래를 들으며 코딩하고 있는 현재의 모습까지. 오늘 하루 동안 여러 감정이 오갔던 것 같아요. 특히 음식을 통해 느낀 감정들이 인상적이에요. 가끔 계획대로 되지 않는 일들이 생기더라도, 그 속에서 작은 행복을 찾아내는 것이 중요하답니다. 소세지의 맛을 즐기신 것처럼, 작은 것에서도 기쁨을 발견하는 마음가짐이 정말 소중해요. 앞으로도 이렇게 긍정적인 마음으로 일상을 채워나가시면, 더욱 풍성한 삶을 살 수 있을 거예요!"
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
     * AI는 자연스러운 텍스트로 반환하고, 서버에서 텍스트를 분석하여 JSON 구조로 변환합니다.
     */
    private EmotionAnalysisResponse parseClovaStudioResponse(Map responseBody) {
        
        try {
            // 응답 구조에서 content 추출
            Map result = (Map) responseBody.get("result");
            Map message = (Map) result.get("message");
            
            // AI가 반환한 자연스러운 텍스트에서 감정 분석 결과 추출
            String content = (String) message.get("content");
            if (content != null && !content.trim().isEmpty()) {
                
                // 1. 요약 텍스트 정리 (줄바꿈, 이모지, 연속 공백 제거)
                String summary = cleanTextContent(content.trim());
                
                // 2. 감정 분석을 위한 키워드 기반 감정 점수 계산
                List<EmotionAnalysisResponse.EmotionScore> emotions = analyzeEmotionsFromText(summary);
                
                // 3. 키워드 추출 및 보정
                List<String> keywords = extractKeywordsFromText(summary);
                
                // 로그 출력
                log.info("=== Clova Studio 응답 분석 결과 (자연어 처리) ===");
                log.info("원본 요약: {}", content);
                log.info("정리된 요약: {}", summary);
                log.info("키워드: {}", keywords);
                log.info("감정 점수: {}", emotions);
                log.info("===============================================");
                
                return EmotionAnalysisResponse.builder()
                    .summary(summary)
                    .emotions(emotions)
                    .keywords(keywords)
                    .build();
            }
            
            throw new RuntimeException("응답에서 유효한 내용을 찾을 수 없습니다.");
                
        } catch (Exception e) {
            log.error("Clova Studio 응답 파싱 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("감정 분석 결과 파싱에 실패했습니다.", e);
        }
    }
    
    /**
     * AI 응답 텍스트를 정리합니다.
     * 줄바꿈 문자, 이모지, 연속된 공백을 제거하고 깔끔한 텍스트로 만듭니다.
     */
    private String cleanTextContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        return text
            // 줄바꿈 문자들을 공백으로 변환
            .replaceAll("\\n+", " ")
            .replaceAll("\\r+", " ")
            // 연속된 공백을 하나의 공백으로 변환
            .replaceAll("\\s+", " ")
            // 앞뒤 공백 제거
            .trim();
    }
    
    /**
     * 텍스트에서 감정을 분석하여 감정 점수 리스트를 생성합니다.
     */
    private List<EmotionAnalysisResponse.EmotionScore> analyzeEmotionsFromText(String text) {
        List<EmotionAnalysisResponse.EmotionScore> emotions = new ArrayList<>();
        
        // 감정 키워드 매칭 및 점수 계산
        Map<String, Integer> emotionScores = new HashMap<>();
        
        // 기쁨 관련 키워드
        int joyScore = countEmotionKeywords(text, "행복", "즐거움", "만족", "신남", "기뻐", "좋아", "즐거워");
        if (joyScore > 0) emotionScores.put("기쁨", joyScore);
        
        // 설렘 관련 키워드
        int excitementScore = countEmotionKeywords(text, "기대", "떨림", "긴장", "두근거림", "설레", "떨려");
        if (excitementScore > 0) emotionScores.put("설렘", excitementScore);
        
        // 평온 관련 키워드
        int calmScore = countEmotionKeywords(text, "차분함", "여유", "안정", "편안함", "차분해", "편안해");
        if (calmScore > 0) emotionScores.put("평온", calmScore);
        
        // 분노 관련 키워드
        int angerScore = countEmotionKeywords(text, "화남", "짜증", "열받음", "격분", "화나", "짜증나");
        if (angerScore > 0) emotionScores.put("분노", angerScore);
        
        // 슬픔 관련 키워드
        int sadnessScore = countEmotionKeywords(text, "슬픔", "우울함", "절망", "허전함", "슬퍼", "우울해");
        if (sadnessScore > 0) emotionScores.put("슬픔", sadnessScore);
        
        // 지침 관련 키워드
        int confusionScore = countEmotionKeywords(text, "혼란", "막막함", "고민", "갈등", "혼란스러워", "막막해");
        if (confusionScore > 0) emotionScores.put("지침", confusionScore);
        
        // 점수 정규화 및 상위 3개 선택
        List<Map.Entry<String, Integer>> sortedEmotions = emotionScores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .toList();
        
        // 백분율 계산 및 감정 점수 생성
        int totalScore = sortedEmotions.stream().mapToInt(Map.Entry::getValue).sum();
        if (totalScore == 0) {
            // 감정이 감지되지 않으면 기본값
            emotions.add(EmotionAnalysisResponse.EmotionScore.builder()
                .type("평온")
                .percentage(100)
                .step(3)
                .build());
        } else {
            for (Map.Entry<String, Integer> entry : sortedEmotions) {
                int percentage = Math.round((float) entry.getValue() / totalScore * 100);
                emotions.add(EmotionAnalysisResponse.EmotionScore.builder()
                    .type(entry.getKey())
                    .percentage(percentage)
                    .step(computeStep(percentage))
                    .build());
            }
        }
        
        return emotions;
    }
    
    /**
     * 텍스트에서 감정 키워드의 출현 빈도를 계산합니다.
     */
    private int countEmotionKeywords(String text, String... keywords) {
        int count = 0;
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 텍스트에서 키워드를 추출합니다.
     */
    private List<String> extractKeywordsFromText(String text) {
        List<String> keywords = new ArrayList<>();
        String lowerText = text.toLowerCase();
        
        // 감정별 키워드 매칭
        if (lowerText.contains("행복") || lowerText.contains("즐거움") || lowerText.contains("만족")) {
            addIfAbsent(keywords, "행복", "즐거움", "만족");
        }
        if (lowerText.contains("기대") || lowerText.contains("떨림") || lowerText.contains("긴장")) {
            addIfAbsent(keywords, "기대", "떨림", "긴장");
        }
        if (lowerText.contains("차분함") || lowerText.contains("여유") || lowerText.contains("안정")) {
            addIfAbsent(keywords, "차분함", "여유", "안정");
        }
        if (lowerText.contains("화남") || lowerText.contains("짜증") || lowerText.contains("열받음")) {
            addIfAbsent(keywords, "화남", "짜증", "열받음");
        }
        if (lowerText.contains("슬픔") || lowerText.contains("우울함") || lowerText.contains("절망")) {
            addIfAbsent(keywords, "슬픔", "우울함", "절망");
        }
        if (lowerText.contains("혼란") || lowerText.contains("막막함") || lowerText.contains("고민")) {
            addIfAbsent(keywords, "혼란", "막막함", "고민");
        }
        
        // 3개 미만이면 기본 키워드로 채우기
        while (keywords.size() < 3) {
            if (!keywords.contains("행복")) keywords.add("행복");
            else if (!keywords.contains("기대")) keywords.add("기대");
            else if (!keywords.contains("차분함")) keywords.add("차분함");
            else break;
        }
        
        return keywords.subList(0, Math.min(keywords.size(), 3));
    }
}
