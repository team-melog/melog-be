package com.melog.melog.adapter.out.persistence.emotion;

import com.melog.melog.application.port.out.EmotionKeywordPersistencePort;
import com.melog.melog.emotion.domain.EmotionKeyword;
import com.melog.melog.emotion.domain.EmotionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmotionKeywordPersistenceAdapter implements EmotionKeywordPersistencePort {

    private final EmotionKeywordJpaRepository emotionKeywordJpaRepository;

    @Override
    public EmotionKeyword save(EmotionKeyword emotionKeyword) {
        return emotionKeywordJpaRepository.save(emotionKeyword);
    }

    @Override
    public Optional<EmotionKeyword> findById(Long id) {
        return emotionKeywordJpaRepository.findById(id);
    }

    @Override
    public List<EmotionKeyword> findByRecord(EmotionRecord record) {
        return emotionKeywordJpaRepository.findByRecord(record);
    }

    @Override
    public void delete(EmotionKeyword emotionKeyword) {
        emotionKeywordJpaRepository.delete(emotionKeyword);
    }

    @Override
    public void deleteByRecord(EmotionRecord record) {
        emotionKeywordJpaRepository.deleteByRecord(record);
    }
} 