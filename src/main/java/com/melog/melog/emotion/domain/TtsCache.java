package com.melog.melog.emotion.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TTS(Text-To-Speech) 캐시 엔티티
 * 
 * 동일한 텍스트와 음성 설정 조합에 대해 생성된 TTS 파일을 캐시하여
 * 중복 생성을 방지하고 응답 성능을 향상시키는 목적으로 사용됩니다.
 * 
 * 캐시 키는 다음 요소들의 조합으로 생성됩니다:
 * - 원본 텍스트
 * - 음성 타입 (VoiceType)
 * - 음성 톤 설정 (VoiceToner)
 * - 오디오 포맷
 * 
 * @author Melog Team
 * @since 1.0
 */
@Entity
@Table(name = "tts_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TtsCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 캐시 키
     * 텍스트, 음성타입, 음성톤, 포맷의 조합을 해시한 값
     * 빠른 검색을 위해 MD5 해시 사용 (32자리)
     */
    @Column(name = "cache_key", nullable = false, unique = true)
    private String cacheKey;

    /**
     * 원본 텍스트
     * TTS 생성에 사용된 원본 텍스트
     * 디버깅 및 관리 목적으로 저장
     */
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    /**
     * 음성 타입
     * VoiceType enum의 name() 값과 매핑
     */
    @Column(name = "voice_type", nullable = false, length = 50)
    private String voiceType;

    /**
     * 음성 톤 설정
     * VoiceToner 객체를 JSON 형태로 직렬화하여 저장
     * 감정 정보에 따른 음성 특성 설정값 포함
     */
    @Column(name = "voice_toner_json", columnDefinition = "TEXT")
    private String voiceTonerJson;

    /**
     * S3 URL
     * 생성된 TTS 파일이 저장된 S3의 전체 URL
     */
    @Column(name = "s3_url", nullable = false, length = 500)
    private String s3Url;

    /**
     * 파일명
     * S3에 저장된 실제 파일명
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * 파일 크기
     * 생성된 TTS 파일의 크기 (바이트 단위)
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * MIME 타입
     * 생성된 TTS 파일의 MIME 타입 (예: audio/mpeg)
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 생성 시간
     * 캐시 엔트리가 최초 생성된 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 마지막 접근 시간
     * 캐시가 마지막으로 사용된 시간
     * 캐시 만료 정책 수립에 활용
     */
    @Column(name = "last_accessed_at", nullable = false)
    private LocalDateTime lastAccessedAt;

    /**
     * TtsCache 엔티티 생성자
     * 
     * @param cacheKey 캐시 키 (필수)
     * @param originalText 원본 텍스트 (필수)
     * @param voiceType 음성 타입 (필수)
     * @param voiceTonerJson 음성 톤 설정 JSON
     * @param s3Url S3 저장 URL (필수)
     * @param fileName 파일명 (필수)
     * @param fileSize 파일 크기
     * @param mimeType MIME 타입
     */
    @Builder
    public TtsCache(String cacheKey, String originalText, String voiceType, 
                   String voiceTonerJson, String s3Url, String fileName, 
                   Long fileSize, String mimeType) {
        this.cacheKey = cacheKey;
        this.originalText = originalText;
        this.voiceType = voiceType;
        this.voiceTonerJson = voiceTonerJson;
        this.s3Url = s3Url;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 마지막 접근 시간을 현재 시간으로 업데이트
     * 캐시 히트 시 호출하여 최근 사용 기록을 갱신
     */
    public void updateLastAccessedTime() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 캐시 엔트리가 지정된 일수 이상 사용되지 않았는지 확인
     * 캐시 정리 정책에 활용
     * 
     * @param days 기준 일수
     * @return 지정된 일수 이상 미사용 여부
     */
    public boolean isNotAccessedForDays(int days) {
        return lastAccessedAt.isBefore(LocalDateTime.now().minusDays(days));
    }
}