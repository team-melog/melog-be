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
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
            EmotionType emotionType = convertToEmotionType(request.getUserSelectedEmotion().getType());
            UserSelectedEmotion userSelectedEmotion = UserSelectedEmotion.builder()
                    .record(savedRecord)
                    .emotionType(emotionType)
                    .percentage(request.getUserSelectedEmotion().getPercentage())
                    .step(2) // 기본값
                    .build();
            userSelectedEmotionPersistencePort.save(userSelectedEmotion);
        }

        // Clova를 통한 감정 분석 수행
        try {
            AnalyzeSentimentRequest sentimentRequest = AnalyzeSentimentRequest.builder()
                    .nickname(nickname)
                    .text(request.getText())
                    .build();
            
            AnalyzeSentimentResponse sentimentResponse = analyzeSentimentUseCase.execute(sentimentRequest);
            
            // 감정 분석 결과로 감정 점수 저장
            if (sentimentResponse.getEmotionResults() != null) {
                for (ExtractEmotionResponse.EmotionResult emotionResult : sentimentResponse.getEmotionResults()) {
                    if (emotionResult.getPercentage() > 0) { // 0% 이상인 감정만 저장
                        EmotionScore emotionScore = EmotionScore.builder()
                                .record(savedRecord)
                                .emotionType(emotionResult.getEmotion())
                                .percentage(emotionResult.getPercentage())
                                .step(1) // AI 분석 단계
                                .build();
                        emotionScorePersistencePort.save(emotionScore);
                    }
                }
            }
            
            // 키워드 저장
            if (sentimentResponse.getKeywords() != null) {
                int weight = sentimentResponse.getKeywords().size();
                for (String keyword : sentimentResponse.getKeywords()) {
                    EmotionKeyword emotionKeyword = EmotionKeyword.builder()
                            .record(savedRecord)
                            .keyword(keyword)
                            .weight(weight--) // 중요도 순으로 가중치 부여
                            .build();
                    emotionKeywordPersistencePort.save(emotionKeyword);
                }
            }
            
            // 요약 정보로 기록 업데이트
            if (sentimentResponse.getSummary() != null) {
                savedRecord.updateRecord(savedRecord.getText(), sentimentResponse.getSummary());
                savedRecord = emotionRecordPersistencePort.save(savedRecord);
            }
            
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
            
            // 변환된 텍스트로 EmotionRecordCreateRequest 생성 (간단한 방법)
            EmotionRecordCreateRequest request = createRequestFromText(text);
            
            // 기존 텍스트 기반 메서드를 호출하여 재사용
            return createEmotionRecord(nickname, request);
            
        } catch (Exception e) {
            log.error("음성 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("음성 파일 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    private EmotionRecordCreateRequest createRequestFromText(String text) {
        EmotionRecordCreateRequest request = new EmotionRecordCreateRequest();
        // 리플렉션을 사용해 text 필드 설정
        try {
            java.lang.reflect.Field textField = request.getClass().getDeclaredField("text");
            textField.setAccessible(true);
            textField.set(request, text);
            return request;
        } catch (Exception e) {
            throw new RuntimeException("Request 생성에 실패했습니다", e);
        }
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
        // TODO: 감정 기록 리스트 조회 (페이징) 구현
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
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
                .emotionScores(emotionScoreResponses)
                .userSelectedEmotion(userSelectedEmotionResponse)
                .emotionKeywords(emotionKeywordResponses)
                .build();
    }

    private EmotionType convertToEmotionType(String type) {
        switch (type) {
            case "기쁨": return EmotionType.JOY;
            case "설렘": return EmotionType.EXCITEMENT;
            case "평온": return EmotionType.CALMNESS;
            case "분노": return EmotionType.ANGER;
            case "슬픔": return EmotionType.SADNESS;
            case "지침": return EmotionType.FATIGUE;
            default: throw new IllegalArgumentException("지원하지 않는 감정 타입입니다: " + type);
        }
    }
} 