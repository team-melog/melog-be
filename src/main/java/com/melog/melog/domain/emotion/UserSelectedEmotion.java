package com.melog.melog.domain.emotion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_selected_emotion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSelectedEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false, unique = true)
    private EmotionRecord record;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_type", nullable = false)
    private EmotionType emotionType;

    @Column(nullable = false)
    private Integer percentage;

    @Column(nullable = false)
    private Integer step;

    @Builder
    public UserSelectedEmotion(EmotionRecord record, EmotionType emotionType, Integer percentage, Integer step) {
        this.record = record;
        this.emotionType = emotionType;
        this.percentage = percentage;
        this.step = step;
    }

    public void updateSelectedEmotion(EmotionType emotionType, Integer percentage, Integer step) {
        this.emotionType = emotionType;
        this.percentage = percentage;
        this.step = step;
    }
} 