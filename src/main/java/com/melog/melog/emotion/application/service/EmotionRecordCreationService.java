package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import com.melog.melog.clova.application.port.in.EmotionAnalysisUseCase;
import com.melog.melog.clova.domain.model.request.EmotionAnalysisRequest;
import com.melog.melog.clova.domain.model.response.EmotionAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordCreationService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final UserSelectedEmotionPersistencePort userSelectedEmotionPersistencePort;
    private final EmotionKeywordPersistencePort emotionKeywordPersistencePort;
    private final EmotionCommentPersistencePort emotionCommentPersistencePort;
    private final EmotionAnalysisUseCase emotionAnalysisUseCase;
    private final ObjectMapper objectMapper;

    /**
     * 텍스트 기반 감정 기록을 생성합니다.
     */
    @Transactional
    public EmotionRecord createEmotionRecordFromText(String nickname, EmotionRecordCreateRequest request) {
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
        performEmotionAnalysis(savedRecord, request.getText());
        
        return savedRecord;
    }

    /**
     * 음성 파일 기반 감정 기록을 생성합니다.
     */
    @Transactional
    public EmotionRecord createEmotionRecordFromAudio(String nickname, String text, String userSelectedEmotionJson, MultipartFile audioFile) {
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
                .text(text)
                .date(today)
                .audioFilePath(audioFilePath)
                .audioFileName(audioFileName)
                .audioDuration(audioDuration)
                .audioFileSize(audioFileSize)
                .audioMimeType(audioMimeType)
                .build();

        EmotionRecord savedRecord = emotionRecordPersistencePort.save(emotionRecord);

        // 사용자 선택 감정 저장
        if (userSelectedEmotionJson != null && !userSelectedEmotionJson.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(userSelectedEmotionJson);
                String type = jsonNode.path("type").asText();
                Integer percentage = jsonNode.path("percentage").asInt();
                
                if (type != null && !type.isEmpty() && percentage != null) {
                    EmotionType emotionType = convertToEmotionType(type);
                    UserSelectedEmotion userSelectedEmotion = UserSelectedEmotion.builder()
                            .record(savedRecord)
                            .emotionType(emotionType)
                            .percentage(percentage)
                            .step(2) // 기본값
                            .build();
                    userSelectedEmotionPersistencePort.save(userSelectedEmotion);
                }
            } catch (Exception e) {
                log.warn("사용자 선택 감정 JSON 파싱 실패: {}", e.getMessage());
            }
        }

        // Clova Studio를 통한 감정 분석 수행
        performEmotionAnalysis(savedRecord, text);
        
        return savedRecord;
    }

    /**
     * 감정 분석을 수행하고 결과를 저장합니다.
     */
    private void performEmotionAnalysis(EmotionRecord record, String text) {
        try {
            EmotionAnalysisRequest emotionRequest = EmotionAnalysisRequest.builder()
                    .text(text)
                    .prompt("감정 요약과 감정 점수 분석")
                    .build();
            
            EmotionAnalysisResponse emotionResponse = emotionAnalysisUseCase.analyzeEmotion(emotionRequest);
            
            // 감정 요약 저장
            record.updateRecord(record.getText(), emotionResponse.getSummary());
            
            // 감정 분석 결과로 감정 점수 저장 및 코멘트 매핑
            for (EmotionAnalysisResponse.EmotionScore emotionScoreData : emotionResponse.getEmotions()) {
                // 한글 감정명을 EmotionType으로 변환
                EmotionType emotionType = convertToEmotionType(emotionScoreData.getType());
                
                // step이 null이면 percentage에 따라 계산
                Integer step = emotionScoreData.getStep();
                if (step == null) {
                    step = calculateStepFromPercentage(emotionScoreData.getPercentage());
                }
                
                EmotionScore emotionScore = EmotionScore.builder()
                        .record(record)
                        .emotionType(emotionType)
                        .percentage(emotionScoreData.getPercentage())
                        .step(step)
                        .build();
                
                // 감정 점수 저장
                emotionScore = emotionScorePersistencePort.save(emotionScore);
                
                // 해당 감정과 단계에 맞는 코멘트 자동 매핑
                try {
                    EmotionComment emotionComment = emotionCommentPersistencePort
                            .findByEmotionTypeAndStep(emotionType, step)
                            .orElse(null);
                    
                    if (emotionComment != null) {
                        emotionScore.updateEmotionComment(emotionComment);
                        emotionScorePersistencePort.save(emotionScore);
                        log.info("감정 코멘트 매핑 완료: emotionType={}, step={}, commentId={}", 
                                emotionType, step, emotionComment.getId());
                    } else {
                        log.warn("감정 코멘트를 찾을 수 없음: emotionType={}, step={}", emotionType, step);
                    }
                } catch (Exception e) {
                    log.warn("감정 코멘트 매핑 실패: emotionType={}, step={}, error={}", 
                            emotionType, step, e.getMessage());
                }
            }
            
            // 가장 높은 감정 점수를 가진 감정의 코멘트를 EmotionRecord에 설정
            try {
                EmotionScore primaryEmotion = record.getPrimaryEmotion();
                if (primaryEmotion != null) {
                    EmotionComment primaryComment = emotionCommentPersistencePort
                            .findByEmotionTypeAndStep(primaryEmotion.getEmotionType(), primaryEmotion.getStep())
                            .orElse(null);
                    
                    if (primaryComment != null) {
                        record.updateEmotionComment(primaryComment);
                        // EmotionRecord를 다시 저장하여 코멘트 정보를 DB에 반영
                        emotionRecordPersistencePort.save(record);
                        log.info("주요 감정 코멘트 매핑 완료: emotionType={}, step={}, commentId={}", 
                                primaryEmotion.getEmotionType(), primaryEmotion.getStep(), primaryComment.getId());
                    } else {
                        log.warn("주요 감정에 해당하는 코멘트를 찾을 수 없음: emotionType={}, step={}", 
                                primaryEmotion.getEmotionType(), primaryEmotion.getStep());
                    }
                }
            } catch (Exception e) {
                log.error("주요 감정 코멘트 매핑 실패: error={}", e.getMessage(), e);
            }
            
            // 키워드 저장
            if (emotionResponse.getKeywords() != null && !emotionResponse.getKeywords().isEmpty()) {
                for (int i = 0; i < emotionResponse.getKeywords().size(); i++) {
                    String keywordText = emotionResponse.getKeywords().get(i);
                    // 키워드 순서에 따라 weight 부여 (첫 번째가 가장 중요)
                    Integer weight = emotionResponse.getKeywords().size() - i;
                    
                    EmotionKeyword emotionKeyword = EmotionKeyword.builder()
                            .record(record)
                            .keyword(keywordText)
                            .weight(weight)
                            .build();
                    emotionKeywordPersistencePort.save(emotionKeyword);
                }
            }
            
            // 요약 정보로 기록 업데이트
            record.updateRecord(record.getText(), emotionResponse.getSummary());
            emotionRecordPersistencePort.save(record);
            
        } catch (Exception e) {
            // 감정 분석 실패 시 로그 남기고 계속 진행
            log.error("감정 분석 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 음성 파일을 로컬에 저장합니다.
     */
    private String saveAudioFileLocally(MultipartFile audioFile, String originalFileName) {
        try {
            // 임시 디렉토리 사용 (권한 문제 해결)
            String uploadDir = "/tmp/melog_audio";
            
            // 디렉토리가 존재하지 않으면 생성
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    log.error("디렉토리 생성 실패: {}", uploadDir);
                    return "디렉토리 생성 실패";
                }
            }
            
            // 디렉토리 쓰기 권한 확인
            if (!directory.canWrite()) {
                log.error("디렉토리 쓰기 권한 없음: {}", uploadDir);
                return "디렉토리 쓰기 권한 없음";
            }

            // 파일명 생성 (타임스탬프 + 원본파일명)
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = timestamp + "_" + originalFileName;
            String filePath = uploadDir + "/" + fileName;

            // 파일 저장
            File destFile = new File(filePath);
            audioFile.transferTo(destFile);
            
            // 파일이 실제로 저장되었는지 확인
            if (!destFile.exists()) {
                log.error("파일 저장 실패: {}", filePath);
                return "파일 저장 실패";
            }

            log.info("음성 파일 저장 완료: {} (크기: {} bytes)", filePath, destFile.length());
            
            return filePath;
        } catch (Exception e) {
            log.error("음성 파일 저장 중 오류 발생: {}", e.getMessage(), e);
            return "파일 저장 오류: " + e.getMessage();
        }
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
     * percentage에 따라 step을 계산합니다.
     * 0-20점: step1, 21-40점: step2, 41-60점: step3, 61-80점: step4, 81-100점: step5
     */
    private Integer calculateStepFromPercentage(Integer percentage) {
        if (percentage == null) {
            return 1;
        }
        
        if (percentage <= 20) {
            return 1;
        } else if (percentage <= 40) {
            return 2;
        } else if (percentage <= 60) {
            return 3;
        } else if (percentage <= 80) {
            return 4;
        } else {
            return 5;
        }
    }
}
