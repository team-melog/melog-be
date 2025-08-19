package com.melog.melog.emotion.domain.model.response;

import com.melog.melog.clova.domain.model.VoiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 오디오 조회/생성 응답 DTO
 * 
 * 오디오 파일 정보와 메타데이터를 클라이언트에게 반환하는 응답 객체입니다.
 * 사용자 업로드 파일과 TTS 생성 파일 모두를 지원하며,
 * 파일의 출처와 상세 정보를 포함합니다.
 * 
 * @author Melog Team
 * @since 1.0
 */
@Data
@Builder
@Schema(description = "오디오 파일 정보 응답")
public class AudioResponse {

    /**
     * 오디오 파일 URL
     * 클라이언트가 오디오 파일에 접근할 수 있는 전체 URL
     * S3 서명된 URL 또는 CDN URL 형태
     */
    @Schema(description = "오디오 파일 URL", example = "https://s3.amazonaws.com/bucket/audio/file.wav")
    private String audioUrl;

    /**
     * 파일명
     * 원본 파일명 또는 생성된 파일명
     * 클라이언트에서 다운로드 시 사용할 수 있는 의미있는 이름
     */
    @Schema(description = "파일명", example = "emotion_audio_20231201.wav")
    private String fileName;

    /**
     * 파일 크기
     * 오디오 파일의 크기 (바이트 단위)
     * 클라이언트에서 다운로드 진행률 표시 등에 활용
     */
    @Schema(description = "파일 크기 (바이트)", example = "1024000")
    private Long fileSize;

    /**
     * MIME 타입
     * 오디오 파일의 MIME 타입 (예: audio/wav, audio/mpeg)
     * 클라이언트에서 적절한 플레이어 선택에 활용
     */
    @Schema(description = "MIME 타입", example = "audio/wav")
    private String mimeType;

    /**
     * 사용자 업로드 파일 여부
     * 
     * true: 사용자가 직접 업로드한 원본 음성 파일
     * false: TTS로 생성된 음성 파일
     * 
     * 클라이언트에서 UI 표시나 추가 기능 제공 시 참고
     */
    @Schema(description = "사용자 업로드 파일 여부", example = "false")
    private Boolean isFromUserUpload;

    /**
     * TTS 음성 타입
     * TTS로 생성된 경우에만 값이 설정됨
     * 사용자 업로드 파일인 경우 null
     */
    @Schema(description = "TTS 음성 타입", example = "ARA")
    private VoiceType voiceType;

    /**
     * 오디오 재생 시간
     * 오디오 파일의 재생 시간 (초 단위)
     * 현재는 사용자 업로드 파일에서만 제공 (TTS는 생성 후 분석 필요)
     */
    @Schema(description = "오디오 재생 시간 (초)", example = "120")
    private Integer duration;

    /**
     * 사용자 업로드 오디오 응답 생성
     * 
     * @param audioUrl 오디오 파일 URL
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param mimeType MIME 타입
     * @param duration 재생 시간
     * @return 사용자 업로드 오디오 응답
     */
    public static AudioResponse fromUserUpload(String audioUrl, String fileName, 
                                             Long fileSize, String mimeType, Integer duration) {
        return AudioResponse.builder()
                .audioUrl(audioUrl)
                .fileName(fileName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .duration(duration)
                .isFromUserUpload(true)
                .voiceType(null) // 사용자 업로드는 음성 타입 없음
                .build();
    }

    /**
     * TTS 생성 오디오 응답 생성
     * 
     * @param audioUrl 오디오 파일 URL
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param mimeType MIME 타입
     * @param voiceType 사용된 음성 타입
     * @return TTS 생성 오디오 응답
     */
    public static AudioResponse fromTtsGeneration(String audioUrl, String fileName, 
                                                Long fileSize, String mimeType, VoiceType voiceType) {
        return AudioResponse.builder()
                .audioUrl(audioUrl)
                .fileName(fileName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .duration(null) // TTS는 현재 재생시간 미제공
                .isFromUserUpload(false)
                .voiceType(voiceType)
                .build();
    }

    /**
     * TTS 캐시에서 오디오 응답 생성
     * 캐시된 TTS 파일 정보로부터 응답 객체 생성
     * 
     * @param s3Url S3 URL
     * @param fileName 파일명
     * @param fileSize 파일 크기
     * @param mimeType MIME 타입
     * @param voiceType 음성 타입
     * @return TTS 캐시 오디오 응답
     */
    public static AudioResponse fromTtsCache(String s3Url, String fileName, Long fileSize, 
                                           String mimeType, VoiceType voiceType) {
        return AudioResponse.builder()
                .audioUrl(s3Url)
                .fileName(fileName)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .duration(null) // 캐시에는 재생시간 정보 없음
                .isFromUserUpload(false)
                .voiceType(voiceType)
                .build();
    }

    /**
     * 오디오 파일이 사용 가능한지 확인
     * URL이 존재하고 비어있지 않은 경우 사용 가능으로 판단
     * 
     * @return 오디오 파일 사용 가능 여부
     */
    public boolean isAudioAvailable() {
        return audioUrl != null && !audioUrl.trim().isEmpty();
    }

    /**
     * TTS 생성 파일인지 확인
     * 
     * @return TTS 생성 파일 여부
     */
    public boolean isTtsGenerated() {
        return !Boolean.TRUE.equals(isFromUserUpload);
    }

    /**
     * 파일 크기를 MB 단위로 반환
     * 클라이언트에서 사용자 친화적인 표시용
     * 
     * @return 파일 크기 (MB 단위, 소수점 2자리)
     */
    public Double getFileSizeInMB() {
        if (fileSize == null) {
            return null;
        }
        return Math.round(fileSize / (1024.0 * 1024.0) * 100.0) / 100.0;
    }
}