package com.melog.melog.application.service;

import com.melog.melog.application.port.in.EmotionRecordUseCase;
import com.melog.melog.application.port.out.*;
import com.melog.melog.domain.emotion.*;
import com.melog.melog.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.domain.model.request.EmotionRecordUpdateRequest;
import com.melog.melog.domain.model.response.*;
import com.melog.melog.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordService implements EmotionRecordUseCase {

    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final UserSelectedEmotionPersistencePort userSelectedEmotionPersistencePort;
    private final EmotionKeywordPersistencePort emotionKeywordPersistencePort;

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecord(Long userId, EmotionRecordCreateRequest request) {
        // 사용자 조회
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 해당 날짜에 이미 기록이 있는지 확인
        if (emotionRecordPersistencePort.existsByUserAndDate(user, request.getDate())) {
            throw new IllegalArgumentException("해당 날짜에 이미 감정 기록이 존재합니다: " + request.getDate());
        }

        // 감정 기록 생성
        EmotionRecord emotionRecord = EmotionRecord.builder()
                .user(user)
                .text(request.getText())
                .summary(request.getSummary())
                .date(request.getDate())
                .build();

        EmotionRecord savedRecord = emotionRecordPersistencePort.save(emotionRecord);

        // 사용자 선택 감정 저장
        if (request.getSelectedEmotion() != null) {
            UserSelectedEmotion userSelectedEmotion = UserSelectedEmotion.builder()
                    .record(savedRecord)
                    .emotionType(request.getSelectedEmotion())
                    .percentage(request.getSelectedEmotionPercentage())
                    .step(request.getSelectedEmotionStep())
                    .build();
            userSelectedEmotionPersistencePort.save(userSelectedEmotion);
        }

        // TODO: 외부 NAVER API를 통한 감정 분석 및 키워드 추출
        // - STT 처리 (음성 파일이 있는 경우)
        // - 감정 분석 (감정 점수 생성)
        // - 키워드 추출
        // - 요약 생성

        return getEmotionRecord(savedRecord.getId());
    }

    @Override
    public EmotionRecordResponse getEmotionRecord(Long recordId) {
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        return buildEmotionRecordResponse(record);
    }

    @Override
    public List<EmotionRecordResponse> getEmotionRecordsByUser(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<EmotionRecord> records = emotionRecordPersistencePort.findByUser(user);

        return records.stream()
                .map(this::buildEmotionRecordResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EmotionRecordResponse getEmotionRecordByUserAndDate(Long userId, LocalDate date) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        EmotionRecord record = emotionRecordPersistencePort.findByUserAndDate(user, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 감정 기록을 찾을 수 없습니다: " + date));

        return buildEmotionRecordResponse(record);
    }

    @Override
    @Transactional
    public EmotionRecordResponse updateEmotionRecord(Long recordId, EmotionRecordUpdateRequest request) {
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        record.updateRecord(request.getText(), request.getSummary());
        EmotionRecord updatedRecord = emotionRecordPersistencePort.save(record);

        return buildEmotionRecordResponse(updatedRecord);
    }

    @Override
    @Transactional
    public void deleteEmotionRecord(Long recordId) {
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        // 연관 데이터 삭제
        emotionScorePersistencePort.deleteByRecord(record);
        userSelectedEmotionPersistencePort.deleteByRecord(record);
        emotionKeywordPersistencePort.deleteByRecord(record);

        // 감정 기록 삭제
        emotionRecordPersistencePort.delete(record);
    }

    private EmotionRecordResponse buildEmotionRecordResponse(EmotionRecord record) {
        // 사용자 정보
        UserResponse userResponse = UserResponse.builder()
                .id(record.getUser().getId())
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
} 