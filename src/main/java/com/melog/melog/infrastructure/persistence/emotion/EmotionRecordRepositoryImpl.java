package com.melog.melog.infrastructure.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.repository.EmotionRecordRepository;
import com.melog.melog.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmotionRecordRepositoryImpl implements EmotionRecordRepository {

    private final EmotionRecordJpaRepository emotionRecordJpaRepository;

    @Override
    public EmotionRecord save(EmotionRecord emotionRecord) {
        return emotionRecordJpaRepository.save(emotionRecord);
    }

    @Override
    public Optional<EmotionRecord> findById(Long id) {
        return emotionRecordJpaRepository.findById(id);
    }

    @Override
    public List<EmotionRecord> findByUser(User user) {
        return emotionRecordJpaRepository.findByUser(user);
    }

    @Override
    public Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date) {
        return emotionRecordJpaRepository.findByUserAndDate(user, date);
    }

    @Override
    public void delete(EmotionRecord emotionRecord) {
        emotionRecordJpaRepository.delete(emotionRecord);
    }

    @Override
    public boolean existsByUserAndDate(User user, LocalDate date) {
        return emotionRecordJpaRepository.existsByUserAndDate(user, date);
    }
} 