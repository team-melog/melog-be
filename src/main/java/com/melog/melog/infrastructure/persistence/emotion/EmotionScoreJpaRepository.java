package com.melog.melog.infrastructure.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.EmotionScore;
import com.melog.melog.domain.emotion.EmotionType;
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
} 