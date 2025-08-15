package com.melog.melog.emotion.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EmotionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_type", nullable = false)
    private EmotionType emotionType;

    @Column(name = "step", nullable = false)
    private Integer step; // 1-5단계 (EmotionScore의 step과 동일)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment; // 감정별 단계별 코멘트

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 활성화 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public EmotionComment(EmotionType emotionType, Integer step, String comment) {
        this.emotionType = emotionType;
        this.step = step;
        this.comment = comment;
        this.isActive = true;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    /**
     * 감정 타입과 단계가 일치하는지 확인합니다.
     */
    public boolean matches(EmotionType emotionType, Integer step) {
        return this.emotionType.equals(emotionType) && this.step.equals(step);
    }
}
