package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.domain.*;
import com.melog.melog.emotion.application.port.out.*;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordManagementService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final EmotionCommentPersistencePort emotionCommentPersistencePort;
    private final EmotionRecordQueryService emotionRecordQueryService;

    /**
     * 감정 선택을 업데이트합니다.
     */
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
            // percentage에 따라 step 계산
            int step = calculateStepFromPercentage(selection.getPercentage());
            
            EmotionScore emotionScore = EmotionScore.builder()
                    .record(record)
                    .emotionType(selection.getType())
                    .percentage(selection.getPercentage())
                    .step(step)
                    .build();
            
            // 감정 점수 저장
            emotionScore = emotionScorePersistencePort.save(emotionScore);
            
            // 해당 감정과 단계에 맞는 코멘트 자동 매핑
            try {
                EmotionComment emotionComment = emotionCommentPersistencePort
                        .findByEmotionTypeAndStep(selection.getType(), step)
                        .orElse(null);
                
                if (emotionComment != null) {
                    emotionScore.updateEmotionComment(emotionComment);
                    emotionScorePersistencePort.save(emotionScore);
                }
            } catch (Exception e) {
                log.warn("감정 코멘트 매핑 실패: emotionType={}, step={}, error={}", 
                        selection.getType(), step, e.getMessage());
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
                        log.info("주요 감정 코멘트 매핑 완료: emotionType={}, step={}, commentId={}", 
                                primaryEmotion.getEmotionType(), primaryEmotion.getStep(), primaryComment.getId());
                    } else {
                        log.warn("주요 감정에 해당하는 코멘트를 찾을 수 없음: emotionType={}, step={}", 
                                primaryEmotion.getEmotionType(), primaryEmotion.getStep());
                    }
                }
            }
        } catch (Exception e) {
            log.error("주요 감정 코멘트 매핑 실패: error={}", e.getMessage(), e);
        }

        // 업데이트된 감정 기록을 다시 조회하여 응답 생성
        EmotionRecord updatedRecord = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("업데이트된 감정 기록을 찾을 수 없습니다: " + recordId));
        
        // 기존 감정 기록 정보를 그대로 사용하여 응답 생성
        // emotions는 이미 위에서 수정했으므로 DB에서 최신 정보 조회
        return emotionRecordQueryService.getEmotionRecord(nickname, recordId);
    }

    /**
     * 감정 기록 텍스트를 업데이트합니다.
     */
    @Transactional
    public EmotionRecordResponse updateEmotionText(String nickname, Long recordId, EmotionRecordTextUpdateRequest request) {
        // 사용자 및 기록 조회
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
        
        EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

        record.updateRecord(request.getText(), null);
        emotionRecordPersistencePort.save(record);

        // 기존 감정 기록 정보를 그대로 사용하여 응답 생성
        // text만 수정했으므로 DB에서 최신 정보 조회
        return emotionRecordQueryService.getEmotionRecord(nickname, recordId);
    }

    /**
     * 감정 기록을 삭제합니다.
     */
    @Transactional
    public void deleteEmotionRecord(String nickname, Long recordId) {
        try {
            // 사용자 및 기록 조회
            User user = userPersistencePort.findByNickname(nickname)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));
            
            EmotionRecord record = emotionRecordPersistencePort.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("감정 기록을 찾을 수 없습니다: " + recordId));

            // 감정 기록 삭제
            emotionRecordPersistencePort.delete(record);
            log.info("감정 기록 삭제 완료: recordId={}, nickname={}", recordId, nickname);
            
        } catch (Exception e) {
            log.error("감정 기록 삭제 실패: recordId={}, nickname={}, error={}", recordId, nickname, e.getMessage(), e);
            throw new RuntimeException("감정 기록 삭제 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * percentage에 따라 step을 계산합니다.
     * 0-20점: step1, 21-40점: step2, 41-60점: step3, 61-80점: step4, 81-100점: step5
     */
    private int calculateStepFromPercentage(Integer percentage) {
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
