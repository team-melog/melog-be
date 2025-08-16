package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import com.melog.melog.clova.application.port.in.EmotionAnalysisUseCase;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionInsightService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionAnalysisUseCase emotionAnalysisUseCase;

    /**
     * 감정 인사이트를 생성합니다.
     */
    public EmotionInsightResponse getEmotionInsight(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        var startDate = month.atDay(1);
        var endDate = month.atEndOfMonth();

        // 해당 월의 감정 기록 조회
        List<EmotionRecord> monthlyRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, startDate, endDate);

        if (monthlyRecords.isEmpty()) {
            return EmotionInsightResponse.builder()
                    .topKeywords(new ArrayList<>())
                    .monthlyComment("이번 달에는 감정 기록이 없습니다.")
                    .build();
        }

        // 모든 감정 기록의 텍스트를 하나로 합치기
        StringBuilder allTexts = new StringBuilder();
        for (EmotionRecord record : monthlyRecords) {
            if (record.getText() != null && !record.getText().trim().isEmpty()) {
                allTexts.append(record.getText()).append(" ");
            }
        }

        String combinedText = allTexts.toString().trim();
        if (combinedText.isEmpty()) {
            return EmotionInsightResponse.builder()
                    .topKeywords(new ArrayList<>())
                    .monthlyComment("이번 달에는 분석할 텍스트가 없습니다.")
                    .build();
        }

        try {
            // Clova Studio를 통한 키워드 추출 및 요약
            EmotionAnalysisRequest emotionRequest = EmotionAnalysisRequest.builder()
                    .text(combinedText)
                    .prompt("이 텍스트들에서 가장 중요한 키워드 5개를 추출하고, 각 키워드의 중요도(1-100)를 계산한 후, 전체 내용을 3줄로 요약해주세요. 응답은 JSON 형태로 제공해주세요.")
                    .build();
            
            var emotionResponse = emotionAnalysisUseCase.analyzeEmotion(emotionRequest);
            
            // 응답에서 키워드와 요약 추출
            List<EmotionKeywordResponse> topKeywords = extractKeywordsFromResponse(emotionResponse.getSummary());
            String monthlyComment = extractSummaryFromResponse(emotionResponse.getSummary());
            
            return EmotionInsightResponse.builder()
                    .topKeywords(topKeywords)
                    .monthlyComment(monthlyComment)
                    .build();
                    
        } catch (Exception e) {
            log.error("감정 인사이트 생성 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시 기본 응답 반환
            return EmotionInsightResponse.builder()
                    .topKeywords(new ArrayList<>())
                    .monthlyComment("감정 인사이트를 생성하는 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * Clova Studio 응답에서 키워드 정보를 추출합니다.
     */
    private List<EmotionKeywordResponse> extractKeywordsFromResponse(String response) {
        List<EmotionKeywordResponse> keywords = new ArrayList<>();
        
        try {
            // JSON 응답에서 키워드 정보 추출
            if (response.contains("키워드") || response.contains("keyword")) {
                // 간단한 파싱 로직 (실제로는 더 정교한 파싱이 필요할 수 있음)
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.contains(":") && (line.contains("키워드") || line.contains("keyword"))) {
                        String[] parts = line.split(":");
                        if (parts.length >= 2) {
                            String keyword = parts[1].trim();
                            // 가중치는 기본값 50으로 설정
                            keywords.add(EmotionKeywordResponse.builder()
                                    .keyword(keyword)
                                    .weight(50)
                                    .build());
                            
                            if (keywords.size() >= 5) break; // 최대 5개까지만
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("키워드 추출 중 오류 발생: {}", e.getMessage());
        }
        
        // 키워드가 추출되지 않은 경우 기본 키워드 반환
        if (keywords.isEmpty()) {
            keywords.add(EmotionKeywordResponse.builder()
                    .keyword("감정 기록")
                    .weight(100)
                    .build());
        }
        
        return keywords;
    }

    /**
     * Clova Studio 응답에서 요약 정보를 추출합니다.
     */
    private String extractSummaryFromResponse(String response) {
        try {
            // 응답에서 요약 부분 추출
            if (response.contains("요약") || response.contains("summary")) {
                String[] lines = response.split("\n");
                StringBuilder summary = new StringBuilder();
                boolean inSummary = false;
                int lineCount = 0;
                
                for (String line : lines) {
                    if (line.contains("요약") || line.contains("summary")) {
                        inSummary = true;
                        continue;
                    }
                    
                    if (inSummary && line.trim().length() > 0 && lineCount < 3) {
                        summary.append(line.trim()).append(" ");
                        lineCount++;
                    }
                    
                    if (lineCount >= 3) break;
                }
                
                if (summary.length() > 0) {
                    return summary.toString().trim();
                }
            }
        } catch (Exception e) {
            log.warn("요약 추출 중 오류 발생: {}", e.getMessage());
        }
        
        // 요약이 추출되지 않은 경우 기본 요약 반환
        return "이번 달의 감정 기록을 분석한 결과입니다.";
    }
}
