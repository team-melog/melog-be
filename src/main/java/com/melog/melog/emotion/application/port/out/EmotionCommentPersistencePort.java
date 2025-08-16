package com.melog.melog.emotion.application.port.out;

import com.melog.melog.emotion.domain.EmotionComment;
import com.melog.melog.emotion.domain.EmotionType;

import java.util.List;
import java.util.Optional;

public interface EmotionCommentPersistencePort {

    /**
     * 감정 타입과 단계에 해당하는 코멘트를 조회합니다.
     */
    Optional<EmotionComment> findByEmotionTypeAndStep(EmotionType emotionType, Integer step);

    /**
     * 감정 타입에 해당하는 모든 활성화된 코멘트를 조회합니다.
     */
    List<EmotionComment> findByEmotionTypeAndIsActiveTrue(EmotionType emotionType);

    /**
     * 모든 활성화된 코멘트를 조회합니다.
     */
    List<EmotionComment> findAllByIsActiveTrue();

    /**
     * 코멘트를 저장합니다.
     */
    EmotionComment save(EmotionComment emotionComment);

    /**
     * 코멘트를 업데이트합니다.
     */
    EmotionComment update(EmotionComment emotionComment);

    /**
     * 코멘트를 비활성화합니다.
     */
    void deactivate(Long commentId);
}
