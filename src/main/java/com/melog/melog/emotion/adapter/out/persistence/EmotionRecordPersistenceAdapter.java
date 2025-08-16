package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.application.port.out.EmotionRecordPersistencePort;
import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<EmotionRecord> findByUser(User user, Pageable pageable) {
        return emotionRecordJpaRepository.findByUser(user, pageable);
    }

    @Override
    public Optional<EmotionRecord> findByUserAndDate(User user, LocalDate date) {
        return emotionRecordJpaRepository.findByUserAndDate(user, date);
    }

    @Override
    public List<EmotionRecord> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate) {
        return emotionRecordJpaRepository.findByUserAndDateBetween(user, startDate, endDate);
    }

    @Override
    public void delete(EmotionRecord emotionRecord) {
        emotionRecordJpaRepository.delete(emotionRecord);
    }

    @Override
    public boolean existsByUserAndDate(User user, LocalDate date) {
        return emotionRecordJpaRepository.existsByUserAndDate(user, date);
    }

    @Override
    public long countByUser(User user) {
        return emotionRecordJpaRepository.countByUser(user);
    }

    // 음성 파일 관련 메서드들 추가

    @Override
    public List<EmotionRecord> findByUserAndAudioFilePathIsNotNull(User user) {
        return emotionRecordJpaRepository.findByUserAndAudioFilePathIsNotNull(user);
    }

    @Override
    public Optional<EmotionRecord> findByAudioFilePath(String audioFilePath) {
        return emotionRecordJpaRepository.findByAudioFilePath(audioFilePath);
    }

    @Override
    public long countByUserAndAudioFilePathIsNotNull(User user) {
        return emotionRecordJpaRepository.countByUserAndAudioFilePathIsNotNull(user);
    }

    @Override
    public List<EmotionRecord> findByUserAndAudioFileSizeGreaterThan(User user, Long minFileSize) {
        return emotionRecordJpaRepository.findByUserAndAudioFileSizeGreaterThan(user, minFileSize);
    }
} 