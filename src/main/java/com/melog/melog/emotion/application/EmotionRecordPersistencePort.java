package com.melog.melog.application.port.out;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmotionRecordPersistencePort {
    
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
     * 감정 기록 삭제
     */
    void delete(EmotionRecord emotionRecord);
    
    /**
     * 사용자의 특정 날짜 감정 기록 존재 여부 확인
     */
    boolean existsByUserAndDate(User user, LocalDate date);
} 