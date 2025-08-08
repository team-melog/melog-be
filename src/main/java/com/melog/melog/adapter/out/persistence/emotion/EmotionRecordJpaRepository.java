package com.melog.melog.adapter.out.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionRecordJpaRepository extends JpaRepository<EmotionRecord, Long> {
    
    List<EmotionRecord> findByUser(User user);
    
    Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date);
    
    boolean existsByUserAndDate(User user, LocalDate date);
} 