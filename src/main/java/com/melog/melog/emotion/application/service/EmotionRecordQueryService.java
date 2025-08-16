package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordQueryService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final UserSelectedEmotionPersistencePort userSelectedEmotionPersistencePort;

    /**
     * 특정 감정 기록을 조회합니다.
     */
    public EmotionRecordResponse getEmotionRecord(String nickname, Long recordId) {
        // 사용자 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        return buildEmotionRecordResponse(record);
    }

    /**
     * 감정 기록 목록을 조회합니다.
     */
    public EmotionListResponse getEmotionList(String nickname, int page, int size) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 페이징 처리
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // 감정 기록 조회
        Page<EmotionRecord> records = emotionRecordPersistencePort.findByUser(user, pageable);
        
        // 응답 DTO 변환 - EmotionRecordSummaryResponse로 변환
        List<EmotionRecordSummaryResponse> summaryResponses = records.getContent().stream()
                .map(this::buildEmotionRecordSummaryResponse)
                .collect(Collectors.toList());
        
        return EmotionListResponse.builder()
                .content(summaryResponses)
                .totalElements(records.getTotalElements())
                .totalPages(records.getTotalPages())
                .page(page)
                .size(size)
                .hasNext(records.hasNext())
                .hasPrevious(records.hasPrevious())
                .build();
    }

    /**
     * 감정 캘린더를 조회합니다.
     */
    public List<EmotionCalendarResponse> getEmotionCalendar(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        var startDate = month.atDay(1);
        var endDate = month.atEndOfMonth();

        // 해당 월의 감정 기록 조회
        List<EmotionRecord> monthlyRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, startDate, endDate);

        // 날짜별로 감정 기록을 그룹화
        Map<LocalDate, List<EmotionRecord>> recordsByDate = monthlyRecords.stream()
                .collect(Collectors.groupingBy(EmotionRecord::getDate));

        // 해당 월의 모든 날짜에 대해 응답 생성
        List<EmotionCalendarResponse> calendarResponses = new ArrayList<>();
        
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate currentDate = month.atDay(day);
            List<EmotionRecord> recordsForDate = recordsByDate.getOrDefault(currentDate, new ArrayList<>());
            
            // 해당 날짜의 감정 점수들을 수집
            List<EmotionScoreResponse> emotionScores = new ArrayList<>();
            Long recordId = null; // 해당 날짜의 첫 번째 기록 ID
            
            for (EmotionRecord record : recordsForDate) {
                if (recordId == null) {
                    recordId = record.getId(); // 첫 번째 기록의 ID 저장
                }
                
                List<EmotionScore> scores = emotionScorePersistencePort.findByRecord(record);
                for (EmotionScore score : scores) {
                    emotionScores.add(EmotionScoreResponse.builder()
                            .id(score.getId())
                            .emotionType(score.getEmotionType())
                            .percentage(score.getPercentage())
                            .step(score.getStep())
                            .build());
                }
            }
            
            calendarResponses.add(EmotionCalendarResponse.builder()
                    .id(recordId) // 해당 날짜의 첫 번째 기록 ID 또는 null
                    .date(currentDate)
                    .emotions(emotionScores)
                    .build());
        }

        return calendarResponses;
    }

    /**
     * 감정 차트를 조회합니다.
     */
    public EmotionChartResponse getEmotionChart(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        var startDate = month.atDay(1);
        var endDate = month.atEndOfMonth();

        // 해당 월의 감정 기록 조회
        List<EmotionRecord> monthlyRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, startDate, endDate);

        // 이번 달 감정 분포 계산
        Map<String, Integer> thisMonth = calculateEmotionDistribution(monthlyRecords);

        // 지난 달 감정 분포 계산 (비교용)
        YearMonth previousMonth = month.minusMonths(1);
        var previousMonthStart = previousMonth.atDay(1);
        var previousMonthEnd = previousMonth.atEndOfMonth();
        List<EmotionRecord> previousMonthRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, previousMonthStart, previousMonthEnd);
        Map<String, Integer> previousMonthDistribution = calculateEmotionDistribution(previousMonthRecords);

        return EmotionChartResponse.builder()
                .thisMonth(thisMonth)
                .compareWithLastMonth(previousMonthDistribution)
                .build();
    }

    /**
     * 감정 기록 목록에서 감정 분포를 계산합니다.
     */
    private Map<String, Integer> calculateEmotionDistribution(List<EmotionRecord> records) {
        Map<String, Integer> distribution = new HashMap<>();
        
        // 모든 감정 타입을 0으로 초기화
        for (EmotionType emotionType : EmotionType.values()) {
            distribution.put(emotionType.getDescription(), 0);
        }

        // 각 기록의 감정 점수를 합산
        for (EmotionRecord record : records) {
            List<EmotionScore> scores = emotionScorePersistencePort.findByRecord(record);
            for (EmotionScore score : scores) {
                String emotionName = score.getEmotionType().getDescription();
                int currentCount = distribution.getOrDefault(emotionName, 0);
                // 감정 점수가 50% 이상인 경우만 카운트 (주요 감정으로 간주)
                if (score.getPercentage() >= 50) {
                    distribution.put(emotionName, currentCount + 1);
                }
            }
        }

        return distribution;
    }

    /**
     * 감정 기록 응답을 생성합니다.
     */
    private EmotionRecordResponse buildEmotionRecordResponse(EmotionRecord record) {
        // 감정 점수 목록 (상위 3개만 반환)
        List<EmotionScoreResponse> emotionScoreResponses = emotionScorePersistencePort.findByRecord(record)
                .stream()
                .map(score -> EmotionScoreResponse.builder()
                        .id(score.getId())
                        .emotionType(score.getEmotionType())
                        .percentage(score.getPercentage())
                        .step(score.getStep())
                        .build())
                .collect(Collectors.toList());
        
        // 상위 3개 감정을 선택하고 백분율을 정규화
        List<EmotionScoreResponse> normalizedEmotions = normalizeTop3Emotions(emotionScoreResponses);

        // 사용자 선택 감정
        UserSelectedEmotionResponse userSelectedEmotionResponse = userSelectedEmotionPersistencePort.findByRecord(record)
                .map(selected -> UserSelectedEmotionResponse.builder()
                        .id(selected.getId())
                        .emotionType(selected.getEmotionType())
                        .percentage(selected.getPercentage())
                        .step(selected.getStep())
                        .build())
                .orElse(null);

        return EmotionRecordResponse.builder()
                .id(record.getId())
                .text(record.getText())
                .summary(record.getSummary())
                .comment(record.getEmotionComment() != null ? record.getEmotionComment().getComment() : null)
                .date(record.getDate())
                .createdAt(record.getCreatedAt())
                .emotions(normalizedEmotions)
                .userSelectedEmotion(userSelectedEmotionResponse)
                .audioFilePath(record.getAudioFilePath())
                .hasAudioFile(record.getAudioFilePath() != null && !record.getAudioFilePath().isEmpty())
                .build();
    }

    /**
     * 감정 기록 요약 응답을 생성합니다.
     */
    private EmotionRecordSummaryResponse buildEmotionRecordSummaryResponse(EmotionRecord record) {
        // 감정 점수 목록 (상위 3개만 반환)
        List<EmotionScoreResponse> emotionScoreResponses = emotionScorePersistencePort.findByRecord(record)
                .stream()
                .map(score -> EmotionScoreResponse.builder()
                        .id(score.getId())
                        .emotionType(score.getEmotionType())
                        .percentage(score.getPercentage())
                        .step(score.getStep())
                        .build())
                .collect(Collectors.toList());
        
        // 상위 3개 감정을 선택하고 백분율을 정규화
        List<EmotionScoreResponse> normalizedEmotions = normalizeTop3Emotions(emotionScoreResponses);

        return EmotionRecordSummaryResponse.builder()
                .id(record.getId())
                .date(record.getDate())
                .summary(record.getSummary())
                .comment(record.getEmotionComment() != null ? record.getEmotionComment().getComment() : null)
                .emotions(normalizedEmotions)
                .audioFilePath(record.getAudioFilePath())
                .hasAudioFile(record.getAudioFilePath() != null && !record.getAudioFilePath().isEmpty())
                .build();
    }

    /**
     * 상위 3개 감정의 백분율을 정규화하여 총합이 100%가 되도록 합니다.
     */
    private List<EmotionScoreResponse> normalizeTop3Emotions(List<EmotionScoreResponse> emotions) {
        if (emotions == null || emotions.isEmpty()) {
            return emotions;
        }
        
        // 상위 3개만 선택하고 퍼센트 내림차순 정렬
        List<EmotionScoreResponse> top3Emotions = emotions.stream()
                .sorted((a, b) -> Integer.compare(b.getPercentage(), a.getPercentage()))
                .limit(3)
                .collect(Collectors.toList());
        
        if (top3Emotions.size() < 3) {
            return top3Emotions; // 3개 미만이면 그대로 반환
        }
        
        // 상위 3개 감정의 총 퍼센트 계산
        int totalPercentage = top3Emotions.stream()
                .mapToInt(EmotionScoreResponse::getPercentage)
                .sum();
        
        // 각 감정의 비율을 계산하여 새로운 퍼센트 할당
        List<EmotionScoreResponse> normalizedEmotions = new ArrayList<>();
        for (int i = 0; i < top3Emotions.size(); i++) {
            EmotionScoreResponse original = top3Emotions.get(i);
            
            // 마지막 감정은 남은 퍼센트를 모두 할당 (반올림 오차 방지)
            int newPercentage;
            if (i == top3Emotions.size() - 1) {
                newPercentage = 100 - normalizedEmotions.stream()
                        .mapToInt(EmotionScoreResponse::getPercentage)
                        .sum();
            } else {
                // 비율에 따라 새로운 퍼센트 계산
                double ratio = (double) original.getPercentage() / totalPercentage;
                newPercentage = (int) Math.round(ratio * 100);
            }
            
            // 퍼센트가 0 이하가 되지 않도록 보정
            newPercentage = Math.max(1, newPercentage);
            
            normalizedEmotions.add(EmotionScoreResponse.builder()
                    .id(original.getId())
                    .emotionType(original.getEmotionType())
                    .percentage(newPercentage)
                    .step(original.getStep())
                    .build());
        }
        
        // 총합이 100%가 되도록 마지막 감정 조정
        int finalTotal = normalizedEmotions.stream()
                .mapToInt(EmotionScoreResponse::getPercentage)
                .sum();
        
        if (finalTotal != 100) {
            EmotionScoreResponse lastEmotion = normalizedEmotions.get(normalizedEmotions.size() - 1);
            int adjustment = 100 - finalTotal;
            
            normalizedEmotions.set(normalizedEmotions.size() - 1, EmotionScoreResponse.builder()
                    .id(lastEmotion.getId())
                    .emotionType(lastEmotion.getEmotionType())
                    .percentage(lastEmotion.getPercentage() + adjustment)
                    .step(lastEmotion.getStep())
                    .build());
        }
        
        return normalizedEmotions;
    }
}
