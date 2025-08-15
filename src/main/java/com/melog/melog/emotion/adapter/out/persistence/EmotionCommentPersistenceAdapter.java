package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.application.port.out.EmotionCommentPersistencePort;
import com.melog.melog.emotion.domain.EmotionComment;
import com.melog.melog.emotion.domain.EmotionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmotionCommentPersistenceAdapter implements EmotionCommentPersistencePort {

    private final EmotionCommentJpaRepository emotionCommentJpaRepository;

    @Override
    public Optional<EmotionComment> findByEmotionTypeAndStep(EmotionType emotionType, Integer step) {
        return emotionCommentJpaRepository.findActiveByEmotionTypeAndStep(emotionType, step);
    }

    @Override
    public List<EmotionComment> findByEmotionTypeAndIsActiveTrue(EmotionType emotionType) {
        return emotionCommentJpaRepository.findByEmotionTypeAndIsActiveTrue(emotionType);
    }

    @Override
    public List<EmotionComment> findAllByIsActiveTrue() {
        return emotionCommentJpaRepository.findByIsActiveTrue();
    }

    @Override
    public EmotionComment save(EmotionComment emotionComment) {
        return emotionCommentJpaRepository.save(emotionComment);
    }

    @Override
    public EmotionComment update(EmotionComment emotionComment) {
        return emotionCommentJpaRepository.save(emotionComment);
    }

    @Override
    public void deactivate(Long commentId) {
        emotionCommentJpaRepository.findById(commentId)
                .ifPresent(comment -> {
                    comment.deactivate();
                    emotionCommentJpaRepository.save(comment);
                });
    }
}
