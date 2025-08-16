package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.EmotionScore;
import com.melog.melog.emotion.domain.EmotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionScoreJpaRepository extends JpaRepository<EmotionScore, Long> {
    
    List<EmotionScore> findByRecord(EmotionRecord record);
    
    Optional<EmotionScore> findByRecordAndEmotionType(EmotionRecord record, EmotionType emotionType);
    
    @Modifying
    @Query("DELETE FROM EmotionScore e WHERE e.record = :record")
    void deleteByRecord(@Param("record") EmotionRecord record);
    
    // 코멘트 연관관계 관련 메서드들 추가
    
    /**
     * 특정 감정 타입과 단계에 해당하는 감정 점수 조회
     */
    List<EmotionScore> findByEmotionTypeAndStep(EmotionType emotionType, Integer step);
    
    /**
     * 감정 점수에 코멘트가 연결된 것들 조회
     */
    List<EmotionScore> findByEmotionCommentIsNotNull();
    
    /**
     * 특정 감정 타입의 감정 점수들 조회
     */
    List<EmotionScore> findByEmotionType(EmotionType emotionType);
    
    /**
     * 특정 단계의 감정 점수들 조회
     */
    List<EmotionScore> findByStep(Integer step);
    
    /**
     * 감정 타입과 단계로 감정 점수 조회 (코멘트 매핑용)
     */
    @Query("SELECT es FROM EmotionScore es WHERE es.emotionType = :emotionType AND es.step = :step")
    List<EmotionScore> findByEmotionTypeAndStepForComment(@Param("emotionType") EmotionType emotionType, @Param("step") Integer step);
} 