package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.application.port.out.EmotionRecordPersistencePort;
import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmotionRecordPersistenceAdapter implements EmotionRecordPersistencePort {

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