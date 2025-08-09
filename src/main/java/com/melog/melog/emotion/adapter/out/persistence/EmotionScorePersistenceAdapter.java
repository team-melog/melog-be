package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.application.port.out.EmotionScorePersistencePort;
import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.EmotionScore;
import com.melog.melog.emotion.domain.EmotionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmotionScorePersistenceAdapter implements EmotionScorePersistencePort {

    private final EmotionScoreJpaRepository emotionScoreJpaRepository;

    @Override
    public EmotionScore save(EmotionScore emotionScore) {
        return emotionScoreJpaRepository.save(emotionScore);
    }

    @Override
    public Optional<EmotionScore> findById(Long id) {
        return emotionScoreJpaRepository.findById(id);
    }

    @Override
    public List<EmotionScore> findByRecord(EmotionRecord record) {
        return emotionScoreJpaRepository.findByRecord(record);
    }

    @Override
    public Optional<EmotionScore> findByRecordAndEmotionType(EmotionRecord record, EmotionType emotionType) {
        return emotionScoreJpaRepository.findByRecordAndEmotionType(record, emotionType);
    }

    @Override
    public void delete(EmotionScore emotionScore) {
        emotionScoreJpaRepository.delete(emotionScore);
    }

    @Override
    public void deleteByRecord(EmotionRecord record) {
        emotionScoreJpaRepository.deleteByRecord(record);
    }
} 