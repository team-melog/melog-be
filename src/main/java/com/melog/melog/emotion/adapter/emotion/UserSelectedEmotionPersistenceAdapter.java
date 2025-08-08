package com.melog.melog.emotion.adapter.emotion;

import com.melog.melog.emotion.application.UserSelectedEmotionPersistencePort;
import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.UserSelectedEmotion;
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