package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import com.melog.melog.clova.application.port.in.MonthlySummaryUseCase;
import com.melog.melog.clova.domain.model.request.MonthlySummaryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionInsightService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionKeywordPersistencePort emotionKeywordPersistencePort;
    private final EmotionRecordQueryService emotionRecordQueryService;
    private final MonthlySummaryUseCase monthlySummaryUseCase;

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

        try {
            // 차트 서비스를 통해 월별 감정 분포 데이터 가져오기
            EmotionChartResponse chartResponse = emotionRecordQueryService.getEmotionChart(nickname, month);
            
            // 감정 분포 데이터를 Clova Studio에 전달하여 조언 요청
            MonthlySummaryRequest summaryRequest = MonthlySummaryRequest.builder()
                    .monthlyComments(List.of()) // 빈 리스트 (사용하지 않음)
                    .month(month.toString())
                    .prompt("월별 감정 분포 분석")
                    .emotionDistribution(chartResponse.getThisMonth()) // 감정 분포 데이터 추가
                    .build();
            
            var summaryResponse = monthlySummaryUseCase.generateMonthlySummary(summaryRequest);
            
            // 기존 DB의 키워드 데이터 사용 (감정별로 이미 저장되어 있음)
            List<EmotionKeywordResponse> topKeywords = extractTopKeywordsFromMonthlyRecords(monthlyRecords);
            String monthlyComment = summaryResponse.getSummary() + " " + summaryResponse.getAdvice();
            
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
     * 월별 감정 기록에서 상위 키워드를 추출합니다.
     */
    private List<EmotionKeywordResponse> extractTopKeywordsFromMonthlyRecords(List<EmotionRecord> monthlyRecords) {
        List<EmotionKeywordResponse> topKeywords = new ArrayList<>();
        
        try {
            // 모든 감정 기록의 키워드를 수집하여 weight 기준으로 정렬
            Map<String, Integer> keywordWeights = new HashMap<>();
            
            for (EmotionRecord record : monthlyRecords) {
                List<EmotionKeyword> keywords = emotionKeywordPersistencePort.findByRecord(record);
                for (EmotionKeyword keyword : keywords) {
                    String keywordText = keyword.getKeyword();
                    int currentWeight = keywordWeights.getOrDefault(keywordText, 0);
                    keywordWeights.put(keywordText, currentWeight + keyword.getWeight());
                }
            }
            
            // weight가 높은 순으로 정렬하여 상위 5개 선택
            List<Map.Entry<String, Integer>> sortedKeywords = keywordWeights.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            
            for (Map.Entry<String, Integer> entry : sortedKeywords) {
                topKeywords.add(EmotionKeywordResponse.builder()
                        .keyword(entry.getKey())
                        .weight(entry.getValue())
                        .build());
            }
            
        } catch (Exception e) {
            log.warn("키워드 추출 중 오류 발생: {}", e.getMessage());
        }
        
        // 키워드가 추출되지 않은 경우 기본 키워드 반환
        if (topKeywords.isEmpty()) {
            topKeywords.add(EmotionKeywordResponse.builder()
                    .keyword("감정 기록")
                    .weight(100)
                    .build());
        }
        
        return topKeywords;
    }
}
