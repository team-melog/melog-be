package com.melog.melog.infrastructure.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.UserSelectedEmotion;
import com.melog.melog.domain.emotion.repository.UserSelectedEmotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserSelectedEmotionRepositoryImpl implements UserSelectedEmotionRepository {

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