package com.melog.melog.domain.emotion;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emotion_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmotionKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private EmotionRecord record;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private Integer weight;

    @Builder
    public EmotionKeyword(EmotionRecord record, String keyword, Integer weight) {
        this.record = record;
        this.keyword = keyword;
        this.weight = weight;
    }

    public void updateKeyword(String keyword, Integer weight) {
        this.keyword = keyword;
        this.weight = weight;
    }
} 