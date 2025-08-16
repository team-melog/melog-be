package com.melog.melog.emotion.domain;

import com.melog.melog.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emotion_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EmotionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private LocalDate date;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 음성 파일 관련 필드들
    @Column(name = "audio_file_path")
    private String audioFilePath; // S3 경로

    @Column(name = "audio_file_name")
    private String audioFileName; // 원본 파일명

    @Column(name = "audio_duration")
    private Integer audioDuration; // 음성 길이(초)

    @Column(name = "audio_file_size")
    private Long audioFileSize; // 파일 크기(bytes)

    @Column(name = "audio_mime_type")
    private String audioMimeType; // 파일 MIME 타입

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmotionScore> emotionScores = new ArrayList<>();

    @OneToOne(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSelectedEmotion userSelectedEmotion;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmotionKeyword> emotionKeywords = new ArrayList<>();

    // EmotionComment와의 연관관계 (가장 높은 감정 점수를 가진 감정의 코멘트)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_comment_id")
    private EmotionComment emotionComment;

    @Builder
    public EmotionRecord(User user, String text, String summary, LocalDate date, 
                        String audioFilePath, String audioFileName, Integer audioDuration, 
                        Long audioFileSize, String audioMimeType) {
        this.user = user;
        this.text = text;
        this.summary = summary;
        this.date = date;
        this.audioFilePath = audioFilePath;
        this.audioFileName = audioFileName;
        this.audioDuration = audioDuration;
        this.audioFileSize = audioFileSize;
        this.audioMimeType = audioMimeType;
    }

    public void updateRecord(String text, String summary) {
        this.text = text;
        this.summary = summary;
    }

    public void updateAudioInfo(String audioFilePath, String audioFileName, 
                               Integer audioDuration, Long audioFileSize, String audioMimeType) {
        this.audioFilePath = audioFilePath;
        this.audioFileName = audioFileName;
        this.audioDuration = audioDuration;
        this.audioFileSize = audioFileSize;
        this.audioMimeType = audioMimeType;
    }

    public void updateEmotionComment(EmotionComment emotionComment) {
        this.emotionComment = emotionComment;
    }

    /**
     * 가장 높은 감정 점수를 가진 감정을 반환합니다.
     */
    public EmotionScore getPrimaryEmotion() {
        return emotionScores.stream()
                .max((a, b) -> Integer.compare(a.getPercentage(), b.getPercentage()))
                .orElse(null);
    }
} 