package com.melog.melog.adapter.out.persistence.emotion;

import com.melog.melog.application.port.out.UserSelectedEmotionPersistencePort;
import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.UserSelectedEmotion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserSelectedEmotionPersistenceAdapter implements UserSelectedEmotionPersistencePort {

    private final UserSelectedEmotionJpaRepository userSelectedEmotionJpaRepository;

    @Override
    public UserSelectedEmotion save(UserSelectedEmotion userSelectedEmotion) {
        return userSelectedEmotionJpaRepository.save(userSelectedEmotion);
    }

    @Override
    public Optional<UserSelectedEmotion> findById(Long id) {
        return userSelectedEmotionJpaRepository.findById(id);
    }

    @Override
    public Optional<UserSelectedEmotion> findByRecord(EmotionRecord record) {
        return userSelectedEmotionJpaRepository.findByRecord(record);
    }

    @Override
    public void delete(UserSelectedEmotion userSelectedEmotion) {
        userSelectedEmotionJpaRepository.delete(userSelectedEmotion);
    }

    @Override
    public void deleteByRecord(EmotionRecord record) {
        userSelectedEmotionJpaRepository.deleteByRecord(record);
    }
} 