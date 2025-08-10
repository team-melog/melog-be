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
        // TODO: 월별 캘린더 감정 리스트 조회 구현
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Override
    public EmotionChartResponse getEmotionChart(String nickname, YearMonth month) {
        // TODO: 월별 감정 분포 통계 구현
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }

    @Override
    public EmotionInsightResponse getEmotionInsight(String nickname, YearMonth month) {
        // TODO: 월별 키워드 및 한줄 요약 구현
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
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