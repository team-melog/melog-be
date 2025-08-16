package com.melog.melog.emotion.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emotion_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmotionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private EmotionRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_type", nullable = false)
    private EmotionType emotionType;

    @Column(nullable = false)
    private Integer percentage;

    @Column(nullable = false)
    private Integer step;

    // EmotionComment와의 연관관계 (해당 감정과 단계에 맞는 코멘트)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_comment_id")
    private EmotionComment emotionComment;

    @Builder
    public EmotionScore(EmotionRecord record, EmotionType emotionType, Integer percentage, Integer step) {
        this.record = record;
        this.emotionType = emotionType;
        this.percentage = percentage;
        this.step = step;
    }

    public void updateScore(Integer percentage, Integer step) {
        this.percentage = percentage;
        this.step = step;
    }

    public void updateEmotionComment(EmotionComment emotionComment) {
        this.emotionComment = emotionComment;
    }

    /**
     * 감정 타입과 단계가 일치하는지 확인합니다.
     */
    public boolean matches(EmotionType emotionType, Integer step) {
        return this.emotionType.equals(emotionType) && this.step.equals(step);
    }
} 