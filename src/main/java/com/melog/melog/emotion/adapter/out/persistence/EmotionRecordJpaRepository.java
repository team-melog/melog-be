package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionRecordJpaRepository extends JpaRepository<EmotionRecord, Long> {
    
    List<EmotionRecord> findByUser(User user);
    
    Page<EmotionRecord> findByUser(User user, Pageable pageable);
    
    Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date);
    
    List<EmotionRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
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
    
    /**
     * 사용자의 특정 기간 음성 파일이 있는 감정 기록 조회
     */
    @Query("SELECT er FROM EmotionRecord er WHERE er.user = :user AND er.audioFilePath IS NOT NULL AND er.date BETWEEN :startDate AND :endDate")
    List<EmotionRecord> findAudioRecordsByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
} 