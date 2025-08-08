package com.melog.melog.application.port.out;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.UserSelectedEmotion;

import java.util.Optional;

public interface UserSelectedEmotionPersistencePort {
    
    /**
     * 사용자 선택 감정 저장
     */
    UserSelectedEmotion save(UserSelectedEmotion userSelectedEmotion);
    
    /**
     * ID로 사용자 선택 감정 조회
     */
    Optional<UserSelectedEmotion> findById(Long id);
    
    /**
     * 감정 기록의 사용자 선택 감정 조회
     */
    Optional<UserSelectedEmotion> findByRecord(EmotionRecord record);
    
    /**
     * 사용자 선택 감정 삭제
     */
    void delete(UserSelectedEmotion userSelectedEmotion);
    
    /**
     * 감정 기록의 사용자 선택 감정 삭제
     */
    void deleteByRecord(EmotionRecord record);
} 