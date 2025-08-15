package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.user.domain.model.response.UserResponse;
import com.melog.melog.emotion.application.port.in.EmotionRecordUseCase;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.domain.EmotionComment;
import com.melog.melog.emotion.domain.EmotionScore;
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
import java.io.File;

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
    private final EmotionCommentPersistencePort emotionCommentPersistencePort;
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
                    
                    // 감정 점수 저장
                    emotionScore = emotionScorePersistencePort.save(emotionScore);
                    
                    // 해당 감정과 단계에 맞는 코멘트 자동 매핑
                    try {
                        EmotionComment emotionComment = emotionCommentPersistencePort
                                .findByEmotionTypeAndStep(emotionType, emotionScoreData.getStep())
                                .orElse(null);
                        
                        if (emotionComment != null) {
                            emotionScore.updateEmotionComment(emotionComment);
                            emotionScorePersistencePort.save(emotionScore);
                        }
                    } catch (Exception e) {
                        log.warn("감정 코멘트 매핑 실패: emotionType={}, step={}, error={}", 
                                emotionType, emotionScoreData.getStep(), e.getMessage());
                    }
                }
                
                // 가장 높은 감정 점수를 가진 감정의 코멘트를 EmotionRecord에 설정
                try {
                    EmotionScore primaryEmotion = savedRecord.getPrimaryEmotion();
                    if (primaryEmotion != null) {
                        EmotionComment primaryComment = emotionCommentPersistencePort
                                .findByEmotionTypeAndStep(primaryEmotion.getEmotionType(), primaryEmotion.getStep())
                                .orElse(null);
                        
                        if (primaryComment != null) {
                            savedRecord.updateEmotionComment(primaryComment);
                            emotionRecordPersistencePort.save(savedRecord);
                        }
                    }
                } catch (Exception e) {
                    log.warn("주요 감정 코멘트 매핑 실패: error={}", e.getMessage());
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
            
            // 음성 파일 정보를 포함하여 감정 기록 생성
            return createEmotionRecordWithAudioInfo(nickname, request, audioFile);
            
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

    /**
     * 음성 파일 정보를 포함하여 감정 기록을 생성합니다.
     */
    @Transactional
    private EmotionRecordResponse createEmotionRecordWithAudioInfo(String nickname, EmotionRecordCreateRequest request, MultipartFile audioFile) {
        // 사용자 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 오늘 날짜로 감정 기록 생성
        LocalDate today = LocalDate.now();
        
        // 해당 날짜에 이미 기록이 있는지 확인
        if (emotionRecordPersistencePort.existsByUserAndDate(user, today)) {
            throw new IllegalArgumentException("오늘 이미 감정 기록이 존재합니다: " + today);
        }

        // 음성 파일 정보 추출
        String audioFileName = audioFile.getOriginalFilename();
        Long audioFileSize = audioFile.getSize();
        String audioMimeType = audioFile.getContentType();
        
        // 로컬 파일 저장 (개발 환경용)
        String audioFilePath = saveAudioFileLocally(audioFile, audioFileName);
        
        // 음성 길이 계산 (임시로 0 설정, 실제로는 오디오 파일 분석 필요)
        Integer audioDuration = 0; // TODO: 오디오 파일 길이 분석 로직 구현 필요

        // 감정 기록 생성 (음성 파일 정보 포함)
        EmotionRecord emotionRecord = EmotionRecord.builder()
                .user(user)
                .text(request.getText())
                .date(today)
                .audioFilePath(audioFilePath)
                .audioFileName(audioFileName)
                .audioDuration(audioDuration)
                .audioFileSize(audioFileSize)
                .audioMimeType(audioMimeType)
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
            
            // 감정 분석 결과로 감정 점수 저장 및 코멘트 매핑
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
                    
                    // 감정 점수 저장
                    emotionScore = emotionScorePersistencePort.save(emotionScore);
                    
                    // 해당 감정과 단계에 맞는 코멘트 자동 매핑
                    try {
                        EmotionComment emotionComment = emotionCommentPersistencePort
                                .findByEmotionTypeAndStep(emotionType, emotionScoreData.getStep())
                                .orElse(null);
                        
                        if (emotionComment != null) {
                            emotionScore.updateEmotionComment(emotionComment);
                            emotionScorePersistencePort.save(emotionScore);
                        }
                    } catch (Exception e) {
                        log.warn("감정 코멘트 매핑 실패: emotionType={}, step={}, error={}", 
                                emotionType, emotionScoreData.getStep(), e.getMessage());
                    }
                }
                
                // 가장 높은 감정 점수를 가진 감정의 코멘트를 EmotionRecord에 설정
                try {
                    EmotionScore primaryEmotion = savedRecord.getPrimaryEmotion();
                    if (primaryEmotion != null) {
                        EmotionComment primaryComment = emotionCommentPersistencePort
                                .findByEmotionTypeAndStep(primaryEmotion.getEmotionType(), primaryEmotion.getStep())
                                .orElse(null);
                        
                        if (primaryComment != null) {
                            savedRecord.updateEmotionComment(primaryComment);
                            emotionRecordPersistencePort.save(savedRecord);
                        }
                    }
                } catch (Exception e) {
                    log.warn("주요 감정 코멘트 매핑 실패: error={}", e.getMessage());
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
    public EmotionRecordResponse updateEmotionSelection(String nickname, Long recordId, EmotionRecordSelectRequest request) {
        // 사용자 및 기록 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        // 기존 감정 점수 삭제
        emotionScorePersistencePort.deleteByRecord(record);

        // 새로운 감정 점수 저장 및 코멘트 매핑
        for (EmotionRecordSelectRequest.EmotionSelection selection : request.getEmotions()) {
            EmotionScore emotionScore = EmotionScore.builder()
                    .record(record)
                    .emotionType(selection.getType())
                    .percentage(selection.getPercentage())
                    .step(2) // 기본값
                    .build();
            
            // 감정 점수 저장
            emotionScore = emotionScorePersistencePort.save(emotionScore);
            
            // 해당 감정과 단계에 맞는 코멘트 자동 매핑
            try {
                EmotionComment emotionComment = emotionCommentPersistencePort
                        .findByEmotionTypeAndStep(selection.getType(), 2) // 기본 step 2
                        .orElse(null);
                
                if (emotionComment != null) {
                    emotionScore.updateEmotionComment(emotionComment);
                    emotionScorePersistencePort.save(emotionScore);
                }
            } catch (Exception e) {
                log.warn("감정 코멘트 매핑 실패: emotionType={}, step={}, error={}", 
                        selection.getType(), 2, e.getMessage());
            }
        }
        
        // 가장 높은 감정 점수를 가진 감정의 코멘트를 EmotionRecord에 설정
        try {
            // 업데이트된 감정 점수들을 다시 조회
            List<EmotionScore> updatedScores = emotionScorePersistencePort.findByRecord(record);
            if (!updatedScores.isEmpty()) {
                // 가장 높은 퍼센트를 가진 감정 찾기
                EmotionScore primaryEmotion = updatedScores.stream()
                        .max((a, b) -> Integer.compare(a.getPercentage(), b.getPercentage()))
                        .orElse(null);
                
                if (primaryEmotion != null) {
                    EmotionComment primaryComment = emotionCommentPersistencePort
                            .findByEmotionTypeAndStep(primaryEmotion.getEmotionType(), primaryEmotion.getStep())
                            .orElse(null);
                    
                    if (primaryComment != null) {
                        record.updateEmotionComment(primaryComment);
                        emotionRecordPersistencePort.save(record);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("주요 감정 코멘트 매핑 실패: error={}", e.getMessage());
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
        try {
            // 사용자 및 기록 조회
            User user = userPersistencePort.findByNickname(nickname)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
            
            EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

            // 사용자 권한 확인 (자신의 기록만 삭제 가능)
            if (!record.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("자신의 감정 기록만 삭제할 수 있습니다.");
            }

            log.info("감정 기록 삭제 시작 - recordId: {}, nickname: {}", recordId, nickname);

            // Cascade 설정으로 인해 EmotionRecord 삭제 시 연관된 모든 엔티티가 자동 삭제됨
            // - emotionScores (CascadeType.ALL, orphanRemoval = true)
            // - userSelectedEmotion (CascadeType.ALL, orphanRemoval = true)  
            // - emotionKeywords (CascadeType.ALL, orphanRemoval = true)
            emotionRecordPersistencePort.delete(record);
            
            log.info("감정 기록 삭제 완료 - recordId: {}, nickname: {}", recordId, nickname);
            
        } catch (Exception e) {
            log.error("감정 기록 삭제 중 오류 발생 - recordId: {}, nickname: {}, error: {}", 
                    recordId, nickname, e.getMessage(), e);
            throw new RuntimeException("감정 기록 삭제에 실패했습니다: " + e.getMessage(), e);
        }
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
            
            EmotionAnalysisResponse emotionResponse = emotionAnalysisUseCase.analyzeEmotion(emotionRequest);
            
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
                .build();
    }

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
    
    /**
     * 상위 3개 감정의 백분율을 정규화하여 총합이 100%가 되도록 합니다.
     * 예: 분노 30, 기쁨 30, 우울 25 → 분노 35, 기쁨 35, 우울 30
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

    /**
     * 음성 파일을 로컬에 저장합니다.
     * 
     * [S3 대체 시 수정 필요 부분]
     * 1. 메서드명 변경: saveAudioFileLocally() → saveAudioFileToS3()
     * 2. 파일 저장 방식: MultipartFile.transferTo() → S3 업로드 API 호출
     * 3. 경로 반환: 로컬 파일 경로 → S3 URL
     * 4. 의존성 추가: AWS SDK (aws-java-sdk-s3) 추가 필요
     * 5. 설정 추가: application.yml에 S3 설정 (bucket, region, credentials) 추가
     * 
     * 예시 S3 URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
     */
    private String saveAudioFileLocally(MultipartFile audioFile, String originalFileName) {
        try {
            // [S3 대체 시] 이 부분을 S3 업로드 로직으로 교체
            // 저장할 디렉토리 경로 설정 (프로젝트 루트 기준)
            String uploadDir = "uploads/audio";
            
            // 디렉토리가 존재하지 않으면 생성
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    log.warn("디렉토리 생성 실패: {}", uploadDir);
                    // 대체 경로 사용
                    uploadDir = "temp/audio";
                    directory = new File(uploadDir);
                    directory.mkdirs();
                }
            }

            // [S3 대체 시] 파일명 생성 로직은 그대로 사용 가능
            // 파일 이름에 타임스탬프 추가하여 중복 방지
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = timestamp + "_" + originalFileName;
            String filePath = uploadDir + "/" + fileName;

            // [S3 대체 시] 이 부분을 S3 업로드로 교체
            // File destFile = new File(filePath);
            // audioFile.transferTo(destFile);
            // 
            // S3 업로드 예시:
            // String s3Key = "audio/" + fileName;
            // s3Client.putObject(bucketName, s3Key, audioFile.getInputStream());
            // String s3Url = s3Client.getUrl(bucketName, s3Key).toString();

            // [S3 대체 시] 이 부분을 S3 URL 반환으로 교체
            // 로컬 파일 저장 (임시)
            File destFile = new File(filePath);
            audioFile.transferTo(destFile);

            log.info("음성 파일 저장 완료: {} (크기: {} bytes)", filePath, destFile.length());
            
            // [S3 대체 시] return s3Url; 로 변경
            return filePath;
        } catch (Exception e) {
            log.error("음성 파일 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("음성 파일 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }
} 