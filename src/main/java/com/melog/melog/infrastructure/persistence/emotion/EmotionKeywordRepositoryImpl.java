package com.melog.melog.infrastructure.persistence.emotion;

import com.melog.melog.domain.emotion.EmotionKeyword;
import com.melog.melog.domain.emotion.EmotionRecord;
import com.melog.melog.domain.emotion.repository.EmotionKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmotionKeywordRepositoryImpl implements EmotionKeywordRepository {

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