package com.melog.melog.emotion.application.port.out;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * 사용자의 감정 기록을 페이징하여 조회
     */
    Page<EmotionRecord> findByUser(User user, Pageable pageable);
    
    /**
     * 사용자의 특정 날짜 감정 기록 조회
     */
    Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date);
    
    /**
     * 사용자의 특정 기간 감정 기록 조회
     */
    List<EmotionRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    /**
     * 감정 기록 삭제
     */
    void delete(EmotionRecord emotionRecord);
    
    /**
     * 사용자의 특정 날짜 감정 기록 존재 여부 확인
     */
    boolean existsByUserAndDate(User user, LocalDate date);
    
    /**
     * 사용자의 감정 기록 개수 조회
     */
    long countByUser(User user);
    
    // 음성 파일 관련 메서드들 추가
    
    /**
     * 사용자의 음성 파일이 있는 감정 기록 조회
     */
    List<EmotionRecord> findByUserAndAudioFilePathIsNotNull(User user);
    
    /**
     * 특정 음성 파일 경로로 감정 기록 조회
     */
    Optional<EmotionRecord> findByAudioFilePath(String audioFilePath);
    
    /**
     * 사용자의 음성 파일 개수 조회
     */
    long countByUserAndAudioFilePathIsNotNull(User user);
    
    /**
     * 음성 파일 크기별 감정 기록 조회 (AI 보이스 기능용)
     */
    List<EmotionRecord> findByUserAndAudioFileSizeGreaterThan(User user, Long minFileSize);
} 