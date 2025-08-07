package com.melog.melog.domain.emotion.repository;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmotionRecordRepository {
    
    /**
     * 감정 기록 저장
     */
    EmotionRecord save(EmotionRecord emotionRecord);
    
    /**
     * ID로 감정 기록 조회
     */
    Optional<EmotionRecord> findById(Long id);
    
    /**
     * 사용자의 모든 감정 기록 조회
     */
    List<EmotionRecord> findByUser(User user);
    
    /**
     * 사용자의 특정 날짜 감정 기록 조회
     */
    Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date);
    
    /**
     * 사용자의 날짜 범위 감정 기록 조회
     */
    List<EmotionRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    /**
     * 사용자의 최근 감정 기록 조회 (최신순)
     */
    List<EmotionRecord> findByUserOrderByDateDesc(User user);
    
    /**
     * 사용자의 감정 기록 개수 조회
     */
    long countByUser(User user);
    
    /**
     * 감정 기록 삭제
     */
    void delete(EmotionRecord emotionRecord);
    
    /**
     * 사용자의 특정 날짜 감정 기록 존재 여부 확인
     */
    boolean existsByUserAndDate(User user, LocalDate date);
} 