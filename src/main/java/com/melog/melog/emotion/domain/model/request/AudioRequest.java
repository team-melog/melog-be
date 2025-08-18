package com.melog.melog.emotion.domain.model.request;

import com.melog.melog.clova.domain.model.VoiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오디오 조회/생성 요청 DTO
 * 
 * 클라이언트에서 오디오 파일을 요청할 때 사용되는 요청 객체입니다.
 * 사용자 업로드 파일 또는 TTS 생성 파일 중 선택할 수 있으며,
 * TTS 생성 시에는 음성 타입을 지정할 수 있습니다.
 * 
 * 비즈니스 규칙:
 * - isRequiredUserAudio가 true인 경우: 기존 업로드된 음성 파일 반환
 * - isRequiredUserAudio가 false인 경우: TTS로 새로 생성하거나 캐시된 파일 반환
 * - TTS 요청 시 voiceType은 필수값이며, null인 경우 기본값(ARA) 사용
 * 
 * @author Melog Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@Schema(description = "오디오 조회/생성 요청")
public class AudioRequest {

    /**
     * 사용자 닉네임
     * 감정 기록을 조회하기 위한 사용자 식별자
     */
    @Schema(description = "사용자 닉네임", example = "user1", required = true)
    @NotBlank(message = "사용자 닉네임은 필수입니다")
    private String nickname;

    /**
     * 감정 기록 ID
     * 오디오를 생성할 대상 감정 기록의 식별자
     */
    @Schema(description = "감정 기록 ID", example = "123", required = true)
    @NotNull(message = "감정 기록 ID는 필수입니다")
    private Long recordId;

    /**
     * 사용자 업로드 오디오 필요 여부
     * 
     * true: 사용자가 업로드한 원본 음성 파일 반환
     *       EmotionRecord.audioFilePath에 저장된 파일 사용
     * 
     * false: TTS(Text-To-Speech)로 생성된 음성 파일 반환
     *        기존 캐시가 있으면 재사용, 없으면 새로 생성
     * 
     * 이 플래그를 통해 클라이언트는 원본 음성과 TTS 음성 중 선택 가능
     */
    @Schema(description = "사용자 업로드 오디오 필요 여부", example = "false", required = true)
    @NotNull(message = "오디오 타입 선택은 필수입니다")
    private Boolean isRequiredUserAudio;

    /**
     * TTS 음성 타입
     * 
     * isRequiredUserAudio가 false일 때만 사용됩니다.
     * VoiceType enum에 정의된 음성 중 하나를 선택
     * 
     * null인 경우 기본값으로 VoiceType.ARA 사용
     * (비즈니스 요구사항에 따라 기본값 변경 가능)
     */
    @Schema(description = "TTS 음성 타입", example = "ARA")
    private VoiceType voiceType;

    /**
     * TTS 요청을 위한 생성자
     * 
     * @param nickname 사용자 닉네임
     * @param recordId 감정 기록 ID
     * @param isRequiredUserAudio 사용자 업로드 오디오 필요 여부
     * @param voiceType TTS 음성 타입 (TTS 요청 시에만 필요)
     */
    public AudioRequest(String nickname, Long recordId, Boolean isRequiredUserAudio, VoiceType voiceType) {
        this.nickname = nickname;
        this.recordId = recordId;
        this.isRequiredUserAudio = isRequiredUserAudio;
        this.voiceType = voiceType;
    }

    /**
     * 사용자 업로드 오디오 요청을 위한 생성자
     * voiceType은 null로 설정됨 (사용되지 않음)
     * 
     * @param nickname 사용자 닉네임
     * @param recordId 감정 기록 ID
     */
    public AudioRequest(String nickname, Long recordId) {
        this(nickname, recordId, true, null);
    }

    /**
     * TTS 요청인지 확인
     * 
     * @return TTS 생성 요청 여부
     */
    public boolean isTtsRequest() {
        return !Boolean.TRUE.equals(isRequiredUserAudio);
    }

    /**
     * 사용자 업로드 오디오 요청인지 확인
     * 
     * @return 사용자 업로드 오디오 요청 여부
     */
    public boolean isUserUploadRequest() {
        return Boolean.TRUE.equals(isRequiredUserAudio);
    }

    /**
     * TTS 요청 시 사용할 음성 타입 반환
     * null인 경우 기본값(ARA) 반환
     * 
     * @return 사용할 음성 타입
     */
    public VoiceType getVoiceTypeOrDefault() {
        return voiceType != null ? voiceType : VoiceType.ARA;
    }
}