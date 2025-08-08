package com.melog.melog.adapter.out.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionKeyword;
import com.melog.melog.domain.emotion.EmotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionKeywordJpaRepository extends JpaRepository<EmotionKeyword, Long> {
    
    List<EmotionKeyword> findByRecord(EmotionRecord record);
    
    @Modifying
    @Query("DELETE FROM EmotionKeyword e WHERE e.record = :record")
    void deleteByRecord(@Param("record") EmotionRecord record);
} 