package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.user.domain.model.response.UserResponse;
import com.melog.melog.emotion.application.port.in.EmotionRecordUseCase;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.in.SpeechToTextUseCase;
import com.melog.melog.clova.application.port.in.EmotionAnalysisUseCase;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.melog.melog.emotion.domain.model.response.EmotionRecordSummaryResponse;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordService implements EmotionRecordUseCase {

    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final UserSelectedEmotionPersistencePort userSelectedEmotionPersistencePort;
    private final EmotionKeywordPersistencePort emotionKeywordPersistencePort;
    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechToTextUseCase speechToTextUseCase;
    private final EmotionAnalysisUseCase emotionAnalysisUseCase;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecord(String nickname, EmotionRecordCreateRequest request) {
        // 사용자 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 오늘 날짜로 감정 기록 생성
        LocalDate today = LocalDate.now();
        
        // 해당 날짜에 이미 기록이 있는지 확인
        if (emotionRecordPersistencePort.existsByUserAndDate(user, today)) {
            throw new IllegalArgumentException("오늘 이미 감정 기록이 존재합니다: " + today);
        }

        // 감정 기록 생성
        EmotionRecord emotionRecord = EmotionRecord.builder()
                .user(user)
                .text(request.getText())
                .date(today)
                .build();

        EmotionRecord savedRecord = emotionRecordPersistencePort.save(emotionRecord);

        // 사용자 선택 감정 저장
        if (request.getUserSelectedEmotion() != null) {
            EmotionType emotionType = request.getUserSelectedEmotion().getEmotionType();
            UserSelectedEmotion userSelectedEmotion = UserSelectedEmotion.builder()
                    .record(savedRecord)
                    .emotionType(emotionType)
                    .percentage(request.getUserSelectedEmotion().getPercentage())
                    .step(2) // 기본값
                    .build();
            userSelectedEmotionPersistencePort.save(userSelectedEmotion);
        }

        // Clova Studio를 통한 감정 분석 수행
        try {
            EmotionAnalysisRequest emotionRequest = EmotionAnalysisRequest.builder()
                    .text(request.getText())
                    .prompt("감정 요약과 감정 점수 분석")
                    .build();
            
            EmotionAnalysisResponse emotionResponse = emotionAnalysisUseCase.analyzeEmotion(emotionRequest);
            
            // 감정 요약 저장
            savedRecord.updateRecord(savedRecord.getText(), emotionResponse.getSummary());
            
            // 감정 분석 결과로 감정 점수 저장
            if (emotionResponse.getEmotions() != null) {
                for (EmotionAnalysisResponse.EmotionScore emotionScoreData : emotionResponse.getEmotions()) {
                    // 한글 감정명을 EmotionType으로 변환
                    EmotionType emotionType = convertToEmotionType(emotionScoreData.getType());
                    
                    EmotionScore emotionScore = EmotionScore.builder()
                            .record(savedRecord)
                            .emotionType(emotionType)
                            .percentage(emotionScoreData.getPercentage())
                            .step(emotionScoreData.getStep())
                            .build();
                    emotionScorePersistencePort.save(emotionScore);
                }
            }
            
            // 키워드 저장
            if (emotionResponse.getKeywords() != null && !emotionResponse.getKeywords().isEmpty()) {
                for (int i = 0; i < emotionResponse.getKeywords().size(); i++) {
                    String keywordText = emotionResponse.getKeywords().get(i);
                    // 키워드 순서에 따라 weight 부여 (첫 번째가 가장 중요)
                    Integer weight = emotionResponse.getKeywords().size() - i;
                    
                    EmotionKeyword emotionKeyword = EmotionKeyword.builder()
                            .record(savedRecord)
                            .keyword(keywordText)
                            .weight(weight)
                            .build();
                    emotionKeywordPersistencePort.save(emotionKeyword);
                }
            }
            
            // 요약 정보로 기록 업데이트
            savedRecord.updateRecord(savedRecord.getText(), emotionResponse.getSummary());
            savedRecord = emotionRecordPersistencePort.save(savedRecord);
            
        } catch (Exception e) {
            // 감정 분석 실패 시 로그 남기고 계속 진행
            log.error("감정 분석 중 오류 발생: {}", e.getMessage(), e);
        }

        return getEmotionRecord(nickname, savedRecord.getId());
    }

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecordWithAudio(String nickname, MultipartFile audioFile, String userSelectedEmotionJson) {
        try {
            // STT를 통해 음성을 텍스트로 변환
            String text = speechToTextUseCase.recognizeToText(audioFile, "ko-KR");
            log.info("STT 변환 결과: {}", text);
            
            // 변환된 텍스트로 EmotionRecordCreateRequest 생성
            EmotionRecordCreateRequest request = createRequestFromText(text, userSelectedEmotionJson);
            
            // 기존 텍스트 기반 메서드를 호출하여 재사용
            return createEmotionRecord(nickname, request);
            
        } catch (Exception e) {
            log.error("음성 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("음성 파일 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    private EmotionRecordCreateRequest createRequestFromText(String text, String userSelectedEmotionJson) {
        EmotionRecordCreateRequest.UserSelectedEmotion userSelectedEmotion = null;
        
        if (userSelectedEmotionJson != null && !userSelectedEmotionJson.trim().isEmpty()) {
            try {
                // JSON 파싱하여 사용자 선택 감정 정보 추출
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(userSelectedEmotionJson);
                String type = jsonNode.path("type").asText();
                Integer percentage = jsonNode.path("percentage").asInt();
                
                if (type != null && !type.isEmpty() && percentage != null) {
                    userSelectedEmotion = EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                            .type(type)
                            .percentage(percentage)
                            .build();
                }
            } catch (Exception e) {
                log.warn("사용자 선택 감정 JSON 파싱 실패: {}", e.getMessage());
            }
        }
        
        return EmotionRecordCreateRequest.builder()
                .text(text)
                .userSelectedEmotion(userSelectedEmotion)
                .build();
    }

    @Override
    @Transactional
    public EmotionRecordResponse updateEmotionSelection(String nickname, Long recordId, EmotionRecordSelectRequest request) {
        // 사용자 및 기록 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        // 기존 감정 점수 삭제
        emotionScorePersistencePort.deleteByRecord(record);

        // 새로운 감정 점수 저장
        for (EmotionRecordSelectRequest.EmotionSelection selection : request.getEmotions()) {
            EmotionScore emotionScore = EmotionScore.builder()
                    .record(record)
                    .emotionType(selection.getType())
                    .percentage(selection.getPercentage())
                    .step(2) // 기본값
                    .build();
            emotionScorePersistencePort.save(emotionScore);
        }

        return getEmotionRecord(nickname, recordId);
    }

    @Override
    @Transactional
    public EmotionRecordResponse updateEmotionText(String nickname, Long recordId, EmotionRecordTextUpdateRequest request) {
        // 사용자 및 기록 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        record.updateRecord(request.getText(), null);
        EmotionRecord updatedRecord = emotionRecordPersistencePort.save(record);

        return getEmotionRecord(nickname, recordId);
    }

    @Override
    public EmotionRecordResponse getEmotionRecord(String nickname, Long recordId) {
        // 사용자 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        return buildEmotionRecordResponse(record);
    }

    @Override
    @Transactional
    public void deleteEmotionRecord(String nickname, Long recordId) {
        // 사용자 및 기록 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        // 연관 데이터 삭제
        emotionScorePersistencePort.deleteByRecord(record);
        userSelectedEmotionPersistencePort.deleteByRecord(record);
        emotionKeywordPersistencePort.deleteByRecord(record);

        // 감정 기록 삭제
        emotionRecordPersistencePort.delete(record);
    }

    @Override
    public List<EmotionCalendarResponse> getEmotionCalendar(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

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
            for (EmotionRecord record : recordsForDate) {
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
                    .date(currentDate)
                    .emotions(emotionScores)
                    .build());
        }

        return calendarResponses;
    }

    @Override
    public EmotionChartResponse getEmotionChart(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // 해당 월의 감정 기록 조회
        List<EmotionRecord> monthlyRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, startDate, endDate);

        // 이번 달 감정 분포 계산
        Map<String, Integer> thisMonth = calculateEmotionDistribution(monthlyRecords);

        // 지난 달 감정 분포 계산 (비교용)
        YearMonth previousMonth = month.minusMonths(1);
        LocalDate previousMonthStart = previousMonth.atDay(1);
        LocalDate previousMonthEnd = previousMonth.atEndOfMonth();
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

    @Override
    public EmotionInsightResponse getEmotionInsight(String nickname, YearMonth month) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 해당 월의 시작일과 끝일 계산
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        // 해당 월의 감정 기록 조회
        List<EmotionRecord> monthlyRecords = emotionRecordPersistencePort.findByUserAndDateBetween(user, startDate, endDate);

        if (monthlyRecords.isEmpty()) {
            return EmotionInsightResponse.builder()
                    .topKeywords(new ArrayList<>())
                    .monthlySummary("이번 달에는 감정 기록이 없습니다.")
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
                    .monthlySummary("이번 달에는 분석할 텍스트가 없습니다.")
                    .build();
        }

        try {
            // Clova Studio를 통한 키워드 추출 및 요약
            EmotionAnalysisRequest emotionRequest = EmotionAnalysisRequest.builder()
                    .text(combinedText)
                    .prompt("이 텍스트들에서 가장 중요한 키워드 5개를 추출하고, 각 키워드의 중요도(1-100)를 계산한 후, 전체 내용을 3줄로 요약해주세요. 응답은 JSON 형태로 제공해주세요.")
                    .build();
            
            EmotionAnalysisResponse emotionResponse = emotionAnalysisUseCase.analyzeEmotion(emotionRequest);
            
            // 응답에서 키워드와 요약 추출
            List<EmotionKeywordResponse> topKeywords = extractKeywordsFromResponse(emotionResponse.getSummary());
            String monthlySummary = extractSummaryFromResponse(emotionResponse.getSummary());
            
            return EmotionInsightResponse.builder()
                    .topKeywords(topKeywords)
                    .monthlySummary(monthlySummary)
                    .build();
                    
        } catch (Exception e) {
            log.error("감정 인사이트 생성 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시 기본 응답 반환
            return EmotionInsightResponse.builder()
                    .topKeywords(new ArrayList<>())
                    .monthlySummary("감정 인사이트를 생성하는 중 오류가 발생했습니다.")
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

    @Override
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

    private EmotionRecordResponse buildEmotionRecordResponse(EmotionRecord record) {
        // 사용자 정보
        UserResponse userResponse = UserResponse.builder()
                .nickname(record.getUser().getNickname())
                .createdAt(record.getUser().getCreatedAt())
                .build();

        // 감정 점수 목록
        List<EmotionScoreResponse> emotionScoreResponses = emotionScorePersistencePort.findByRecord(record)
                .stream()
                .map(score -> EmotionScoreResponse.builder()
                        .id(score.getId())
                        .emotionType(score.getEmotionType())
                        .percentage(score.getPercentage())
                        .step(score.getStep())
                        .build())
                .collect(Collectors.toList());

        // 사용자 선택 감정
        UserSelectedEmotionResponse userSelectedEmotionResponse = userSelectedEmotionPersistencePort.findByRecord(record)
                .map(selected -> UserSelectedEmotionResponse.builder()
                        .id(selected.getId())
                        .emotionType(selected.getEmotionType())
                        .percentage(selected.getPercentage())
                        .step(selected.getStep())
                        .build())
                .orElse(null);

        // 감정 키워드 목록
        List<EmotionKeywordResponse> emotionKeywordResponses = emotionKeywordPersistencePort.findByRecord(record)
                .stream()
                .map(keyword -> EmotionKeywordResponse.builder()
                        .id(keyword.getId())
                        .keyword(keyword.getKeyword())
                        .weight(keyword.getWeight())
                        .build())
                .collect(Collectors.toList());

        return EmotionRecordResponse.builder()
                .id(record.getId())
                .text(record.getText())
                .summary(record.getSummary())
                .date(record.getDate())
                .createdAt(record.getCreatedAt())
                .user(userResponse)
                .emotions(emotionScoreResponses)
                .userSelectedEmotion(userSelectedEmotionResponse)
                .emotionKeywords(emotionKeywordResponses)
                .build();
    }

    private EmotionRecordSummaryResponse buildEmotionRecordSummaryResponse(EmotionRecord record) {
        // 감정 점수 목록
        List<EmotionScoreResponse> emotionScoreResponses = emotionScorePersistencePort.findByRecord(record)
                .stream()
                .map(score -> EmotionScoreResponse.builder()
                        .id(score.getId())
                        .emotionType(score.getEmotionType())
                        .percentage(score.getPercentage())
                        .step(score.getStep())
                        .build())
                .collect(Collectors.toList());

        return EmotionRecordSummaryResponse.builder()
                .id(record.getId())
                .date(record.getDate())
                .summary(record.getSummary())
                .emotions(emotionScoreResponses)
                .build();
    }

    /**
     * 한글 감정명을 EmotionType enum으로 변환합니다.
     */
    private EmotionType convertToEmotionType(String emotionName) {
        if (emotionName == null) {
            throw new IllegalArgumentException("감정명이 null입니다.");
        }
        
        for (EmotionType emotionType : EmotionType.values()) {
            if (emotionType.getDescription().equals(emotionName)) {
                return emotionType;
            }
        }
        
        throw new IllegalArgumentException("알 수 없는 감정 타입입니다: " + emotionName);
    }
} 