package com.melog.melog.emotion.application;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.EmotionScore;
import com.melog.melog.emotion.domain.EmotionType;

import java.util.List;
import java.util.Optional;

public interface EmotionScorePersistencePort {
    
    /**
     * 감정 점수 저장
     */
    EmotionScore save(EmotionScore emotionScore);
    
    /**
     * ID로 감정 점수 조회
     */
    Optional<EmotionScore> findById(Long id);
    
    /**
     * 감정 기록의 모든 감정 점수 조회
     */
    List<EmotionScore> findByRecord(EmotionRecord record);
    
    /**
     * 감정 기록의 특정 감정 타입 점수 조회
     */
    Optional<EmotionScore> findByRecordAndEmotionType(EmotionRecord record, EmotionType emotionType);
    
    /**
     * 감정 점수 삭제
     */
    void delete(EmotionScore emotionScore);
    
    /**
     * 감정 기록의 모든 감정 점수 삭제
     */
    void deleteByRecord(EmotionRecord record);
} 