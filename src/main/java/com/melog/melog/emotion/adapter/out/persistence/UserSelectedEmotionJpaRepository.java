package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.UserSelectedEmotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSelectedEmotionJpaRepository extends JpaRepository<UserSelectedEmotion, Long> {
    
    Optional<UserSelectedEmotion> findByRecord(EmotionRecord record);
    
    @Modifying
    @Query("DELETE FROM UserSelectedEmotion u WHERE u.record = :record")
    void deleteByRecord(@Param("record") EmotionRecord record);
} 