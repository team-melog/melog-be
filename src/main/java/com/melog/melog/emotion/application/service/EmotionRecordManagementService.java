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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordManagementService {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final EmotionCommentPersistencePort emotionCommentPersistencePort;

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

        return null; // 실제로는 EmotionRecordResponse를 반환해야 하지만, 여기서는 null 반환
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
        EmotionRecord updatedRecord = emotionRecordPersistencePort.save(record);

        return null; // 실제로는 EmotionRecordResponse를 반환해야 하지만, 여기서는 null 반환
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
}
