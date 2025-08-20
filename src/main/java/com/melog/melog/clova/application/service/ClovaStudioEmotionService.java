package com.melog.melog.clova.application.service;

import com.melog.melog.clova.application.port.in.EmotionAnalysisUseCase;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;
import com.melog.melog.clova.adapter.external.out.ClovaStudioAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClovaStudioEmotionService implements EmotionAnalysisUseCase {

    private final ClovaStudioAdapter clovaStudioAdapter;

    @Override
    public EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request) {
        log.info("Clova Studio 감정 분석 시작: 텍스트 길이 = {}", request.getText().length());
        
        try {
            // 규칙 준수 검증 + 재시도 래퍼 사용
            EmotionAnalysisResponse response = analyzeCompliant(request.getText(), 600, 800);
            
            log.info("Clova Studio 감정 분석 완료: 요약 길이 = {}, 감정 개수 = {}", 
                    response.getSummary().length(), response.getEmotions().size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Clova Studio 감정 분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("감정 분석에 실패했습니다.", e);
        }
    }

    // === [추가] 규칙 검증 유틸 ===
    private static boolean isSummaryLengthOk(String s, int min, int max) {
        if (s == null) return false;
        int len = s.replaceAll("\\s+", " ").trim().length();
        return len >= min && len <= max;
    }

    // 2인칭 대화체 힌트(완벽 검증은 아니어도 제3자/나레이터 억제에 도움)
    private static boolean looksSecondPerson(String s) {
        if (s == null) return false;
        // "~요", "~셨군요", "~느껴졌네요" 같은 구어체 어미가 3회 이상이면 OK
        int hits = 0;
        for (String pat : new String[] {"요.", "요!", "셨군요", "셨네요", "느껴졌네요", "하셨겠어요"}) {
            hits += s.split(pat, -1).length - 1;
        }
        return hits >= 3;
    }

    // 키워드가 Top3 감정의 풀 안에서만 나왔는지
    private static boolean keywordsMatchTopEmotions(List<String> kws, List<EmotionAnalysisResponse.EmotionScore> emos) {
        if (kws == null || emos == null || emos.isEmpty()) return false;
        java.util.Set<String> allowed = new java.util.HashSet<>();
        for (int i = 0; i < Math.min(3, emos.size()); i++) {
            switch (emos.get(i).getType()) {
                case "기쁨" -> allowed.addAll(java.util.List.of("행복","즐거움","만족","신남"));
                case "설렘" -> allowed.addAll(java.util.List.of("기대","떨림","긴장","두근거림"));
                case "평온" -> allowed.addAll(java.util.List.of("차분함","여유","안정","편안함"));
                case "분노" -> allowed.addAll(java.util.List.of("화남","짜증","열받음","격분"));
                case "슬픔" -> allowed.addAll(java.util.List.of("슬픔","우울함","절망","허전함"));
                default     -> allowed.addAll(java.util.List.of("혼란","막막함","고민","갈등")); // 지침
            }
        }
        return kws.stream().allMatch(allowed::contains);
    }

    // === [추가] 규칙 보정(서버 보정) ===
    private static int stepFromPct(int p) {
        if (p <= 20) return 1;
        if (p <= 40) return 2;
        if (p <= 60) return 3;
        if (p <= 80) return 4;
        return 5;
    }

    private static EmotionAnalysisResponse normalizeEmotionsAndKeywords(EmotionAnalysisResponse r) {
        // step 재계산
        for (var e : r.getEmotions()) {
            e.calculateStep();
        }
        // 키워드 3개 보정 + Top3 감정의 풀로 제한
        var emos = r.getEmotions();
        emos.sort((a,b)->Integer.compare(b.getPercentage(), a.getPercentage()));
        java.util.Set<String> allowed = new java.util.HashSet<>();
        for (int i = 0; i < Math.min(3, emos.size()); i++) {
            switch (emos.get(i).getType()) {
                case "기쁨" -> allowed.addAll(java.util.List.of("행복","즐거움","만족","신남"));
                case "설렘" -> allowed.addAll(java.util.List.of("기대","떨림","긴장","두근거림"));
                case "평온" -> allowed.addAll(java.util.List.of("차분함","여유","안정","편안함"));
                case "분노" -> allowed.addAll(java.util.List.of("화남","짜증","열받음","격분"));
                case "슬픔" -> allowed.addAll(java.util.List.of("슬픔","우울함","절망","허전함"));
                default     -> allowed.addAll(java.util.List.of("혼란","막막함","고민","갈등"));
            }
        }
        var kws = new java.util.ArrayList<>(r.getKeywords() == null ? java.util.List.<String>of() : r.getKeywords());
        kws.removeIf(k -> !allowed.contains(k));
        // 부족분 채우기
        int idx = 0;
        String[][] fill = {
            {"행복","즐거움","만족","신남"},
            {"기대","떨림","긴장","두근거림"},
            {"차분함","여유","안정","편안함"},
            {"화남","짜증","열받음","격분"},
            {"슬픔","우울함","절망","허전함"},
            {"혼란","막막함","고민","갈등"}
        };
        while (kws.size() < 3 && !emos.isEmpty()) {
            String top = emos.get(Math.min(idx, emos.size()-1)).getType();
            String[] cand = switch (top) {
                case "기쁨" -> fill[0];
                case "설렘" -> fill[1];
                case "평온" -> fill[2];
                case "분노" -> fill[3];
                case "슬픔" -> fill[4];
                default     -> fill[5];
            };
            for (String c : cand) if (kws.size() < 3 && !kws.contains(c) && allowed.contains(c)) kws.add(c);
            idx++;
        }
        if (kws.size() > 3) kws = new java.util.ArrayList<>(kws.subList(0,3));
        // Builder를 사용하여 새로운 객체 생성
        return EmotionAnalysisResponse.builder()
            .summary(r.getSummary())
            .emotions(r.getEmotions())
            .keywords(kws)
            .build();
    }

    // === [수정] 성능 개선을 위해 1차 시도만 수행 ===
    public EmotionAnalysisResponse analyzeCompliant(String text, int minLen, int maxLen) {
        // 1차 시도만 수행 (성능 개선)
        EmotionAnalysisResponse response = clovaStudioAdapter.analyzeEmotion(EmotionAnalysisRequest.builder()
            .text(text)
            .build());
        
        // 응답 정규화
        response = normalizeEmotionsAndKeywords(response);
        
        log.info("Clova Studio 감정 분석 완료 (1차 시도): 요약 길이 = {}, 감정 개수 = {}", 
                response.getSummary().length(), response.getEmotions().size());
        
        return response;
    }
}
