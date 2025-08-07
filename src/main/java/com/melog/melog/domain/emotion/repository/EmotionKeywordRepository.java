package com.melog.melog.domain.emotion.repository;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.EmotionKeyword;

import java.util.List;
import java.util.Optional;

public interface EmotionKeywordRepository {
    
    /**
     * 감정 키워드 저장
     */
    EmotionKeyword save(EmotionKeyword emotionKeyword);
    
    /**
     * ID로 감정 키워드 조회
     */
    Optional<EmotionKeyword> findById(Long id);
    
    /**
     * 감정 기록의 모든 키워드 조회
     */
    List<EmotionKeyword> findByRecord(EmotionRecord record);
    
    /**
     * 감정 기록의 키워드들을 가중치 순으로 조회 (내림차순)
     */
    List<EmotionKeyword> findByRecordOrderByWeightDesc(EmotionRecord record);
    
    /**
     * 특정 키워드로 조회
     */
    Optional<EmotionKeyword> findByRecordAndKeyword(EmotionRecord record, String keyword);
    
    /**
     * 감정 키워드 삭제
     */
    void delete(EmotionKeyword emotionKeyword);
    
    /**
     * 감정 기록의 모든 키워드 삭제
     */
    void deleteByRecord(EmotionRecord record);
} 