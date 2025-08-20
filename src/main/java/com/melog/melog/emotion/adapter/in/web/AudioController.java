package com.melog.melog.emotion.adapter.in.web;

import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.emotion.application.port.in.AudioUseCase;
import com.melog.melog.emotion.domain.model.request.AudioBodyRequest;
import com.melog.melog.emotion.domain.model.request.AudioRequest;
import com.melog.melog.emotion.domain.model.response.AudioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 오디오 관련 REST API 컨트롤러
 *
 * - POST /api/users/{nickname}/emotions/{id}/text : 오디오 조회/생성(원본 or TTS/캐시)
 * - GET  /api/voice/types                        : 음성 타입 목록
 * - GET  /api/audio/health                       : 헬스 체크
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Audio", description = "오디오 파일 조회 및 TTS 생성 API")
public class AudioController {

    private final AudioUseCase audioUseCase;

    /**
     * 오디오 파일 조회 또는 생성 (원본 or TTS/캐시)
     *
     * PathVariable:
     * - nickname : 사용자 닉네임
     * - id       : 감정 기록 ID
     *
     * RequestBody:
     * - isRequiredUserAudio (true=원본, false=TTS)
     * - voiceType (옵션, TTS 시 사용. 미지정 시 서버 기본값 ARA)
     */
    @Operation(
            summary = "오디오 파일 조회 또는 생성",
            description = "사용자 업로드 파일 반환 또는 TTS 생성/캐시(DB 선조회 → 미스 시 생성→S3 저장→DB 기록)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = AudioResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 감정 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/users/{nickname}/emotions/{id}/voice")
    public ResponseEntity<AudioResponse> getOrCreateAudio(
            @Parameter(description = "사용자 닉네임", required = true)
            @PathVariable("nickname") String nickname,
            @Parameter(description = "감정 기록 ID", required = true)
            @PathVariable("id") Long recordId,
            @Parameter(description = "오디오 요청 옵션", required = true)
            @Valid @RequestBody AudioBodyRequest body
    ) {
        log.info("오디오 조회/생성 API 호출: nickname={}, recordId={}, isRequiredUserAudio={}, voiceType={}",
                nickname, recordId, body.getIsRequiredUserAudio(), body.getVoiceType());

        try {
            // Path + Body → 기존 서비스 요청 DTO로 매핑
            AudioRequest request = new AudioRequest(
                    nickname,
                    recordId,
                    body.getIsRequiredUserAudio(),
                    body.getVoiceType()
            );

            AudioResponse response = audioUseCase.getOrCreateAudio(request);

            log.info("오디오 조회/생성 API 완료: nickname={}, recordId={}, audioUrl={}, isFromUserUpload={}",
                    nickname, recordId, response.getAudioUrl(), response.getIsFromUserUpload());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 오디오 요청: nickname={}, recordId={}, error={}", nickname, recordId, e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException e) {
            log.error("오디오 조회/생성 API 오류: nickname={}, recordId={}, error={}", nickname, recordId, e.getMessage(), e);

            if (e.getMessage().contains("찾을 수 없습니다") || e.getMessage().contains("없습니다")) {
                return ResponseEntity.notFound().build(); // 404
            } else if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(403).build(); // 403
            } else {
                return ResponseEntity.internalServerError().build(); // 500
            }
        }
    }

    /**
     * 사용 가능한 음성 타입 목록 조회 (변경 없음)
     */
    @Operation(summary = "음성 타입 목록 조회", description = "TTS 생성에 사용할 수 있는 모든 음성 타입 목록을 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = VoiceTypeResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/voice/types")
    public ResponseEntity<List<VoiceTypeResponse>> getVoiceTypes() {
        log.debug("음성 타입 목록 조회 API 호출");

        try {
            List<VoiceTypeResponse> voiceTypes = Arrays.stream(VoiceType.values())
                    .map(voiceType -> new VoiceTypeResponse(
                            voiceType.name(),        // enum 이름
                            voiceType.getVoiceKey(), // 시스템 키
                            voiceType.getVoiceName() // 표시명
                    ))
                    .collect(Collectors.toList());

            log.debug("음성 타입 목록 조회 완료: count={}", voiceTypes.size());
            return ResponseEntity.ok(voiceTypes);

        } catch (Exception e) {
            log.error("음성 타입 목록 조회 중 오류 발생: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Schema(description = "음성 타입 정보")
    public static class VoiceTypeResponse {
        @Schema(description = "음성 타입 enum 이름", example = "ARA")
        private final String name;

        @Schema(description = "시스템 내부 음성 키", example = "vara")
        private final String voiceKey;

        @Schema(description = "사용자 친화적인 음성 이름", example = "아라")
        private final String voiceName;

        public VoiceTypeResponse(String name, String voiceKey, String voiceName) {
            this.name = name;
            this.voiceKey = voiceKey;
            this.voiceName = voiceName;
        }

        public String getName() { return name; }
        public String getVoiceKey() { return voiceKey; }
        public String getVoiceName() { return voiceName; }
    }

}
