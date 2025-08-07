package com.melog.melog.domain.emotion.repository;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.EmotionScore;
import com.melog.melog.domain.emotion.EmotionType;

import java.util.List;
import java.util.Optional;

public interface EmotionScoreRepository {
    
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