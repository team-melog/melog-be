package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.domain.EmotionComment;
import com.melog.melog.emotion.domain.EmotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmotionCommentJpaRepository extends JpaRepository<EmotionComment, Long> {

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
    List<EmotionComment> findByIsActiveTrue();

    /**
     * 감정 타입과 단계로 활성화된 코멘트를 조회합니다.
     */
    @Query("SELECT ec FROM EmotionComment ec WHERE ec.emotionType = :emotionType AND ec.step = :step AND ec.isActive = true")
    Optional<EmotionComment> findActiveByEmotionTypeAndStep(@Param("emotionType") EmotionType emotionType, @Param("step") Integer step);

    /**
     * 특정 감정 타입의 활성화된 코멘트 개수를 조회합니다.
     */
    long countByEmotionTypeAndIsActiveTrue(EmotionType emotionType);
}
