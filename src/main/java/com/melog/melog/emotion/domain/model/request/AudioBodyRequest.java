package com.melog.melog.emotion.domain.model.request;

import com.melog.melog.clova.domain.model.VoiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오디오 요청 바디(옵션만 담음)
 * - PathVariable: nickname, id(=recordId)
 * - Body: isRequiredUserAudio, voiceType
 */
@Data
@NoArgsConstructor
@Schema(description = "오디오 요청 바디(옵션)")
public class AudioBodyRequest {

    @Schema(description = "사용자 업로드 오디오 필요 여부", example = "false", required = true)
    @NotNull(message = "오디오 타입 선택은 필수입니다")
    private Boolean isRequiredUserAudio;

    @Schema(description = "TTS 음성 타입(옵션, 미지정 시 서버 기본값 ARA)", example = "ARA")
    private VoiceType voiceType;
}