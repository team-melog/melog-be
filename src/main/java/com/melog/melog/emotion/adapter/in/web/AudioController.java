package com.melog.melog.emotion.adapter.in.web;

import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.emotion.application.port.in.AudioUseCase;
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
 * 오디오 파일 조회 및 TTS 생성에 관한 웹 인터페이스를 제공합니다.
 * 클라이언트의 오디오 요청을 처리하고 적절한 HTTP 응답을 반환합니다.
 * 
 * 지원하는 기능:
 * - 사용자 업로드 오디오 파일 조회
 * - TTS(Text-To-Speech) 오디오 생성 및 조회
 * - 사용 가능한 음성 타입 목록 조회
 * 
 * API 엔드포인트:
 * - POST /api/audio/get-or-create: 오디오 조회/생성
 * - GET /api/voice/types: 음성 타입 목록 조회
 * 
 * @author Melog Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Audio", description = "오디오 파일 조회 및 TTS 생성 API")
public class AudioController {

    private final AudioUseCase audioUseCase;

    /**
     * 오디오 파일 조회 또는 생성
     * 
     * 클라이언트의 요청에 따라 사용자 업로드 파일을 반환하거나
     * TTS로 새로운 오디오를 생성합니다. TTS 요청의 경우 캐시를 우선 확인하여
     * 동일한 요청에 대한 중복 처리를 방지합니다.
     * 
     * 요청 예시:
     * - 사용자 업로드 파일: {"nickname": "user1", "recordId": 123, "isRequiredUserAudio": true}
     * - TTS 생성: {"nickname": "user1", "recordId": 123, "isRequiredUserAudio": false, "voiceType": "ARA"}
     * 
     * 응답 구조:
     * - audioUrl: 오디오 파일 접근 URL
     * - fileName: 파일명
     * - fileSize: 파일 크기 (바이트)
     * - mimeType: MIME 타입
     * - isFromUserUpload: 사용자 업로드 여부
     * - voiceType: TTS 음성 타입 (TTS인 경우만)
     * 
     * @param request 오디오 요청 정보
     * @return 오디오 파일 정보 및 메타데이터
     */
    @Operation(
            summary = "오디오 파일 조회 또는 생성",
            description = "사용자 업로드 파일 조회 또는 TTS를 통한 새로운 오디오 생성"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공적으로 오디오 파일 정보를 반환",
                    content = @Content(schema = @Schema(implementation = AudioResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 감정 기록을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/audio/get-or-create")
    public ResponseEntity<AudioResponse> getOrCreateAudio(
            @Parameter(description = "오디오 요청 정보", required = true)
            @Valid @RequestBody AudioRequest request) {
        log.info("오디오 조회/생성 API 호출: nickname={}, recordId={}, isRequiredUserAudio={}, voiceType={}", 
                request.getNickname(), request.getRecordId(), request.getIsRequiredUserAudio(), request.getVoiceType());
        
        try {
            // 유스케이스를 통한 비즈니스 로직 처리
            AudioResponse response = audioUseCase.getOrCreateAudio(request);
            
            log.info("오디오 조회/생성 API 완료: nickname={}, recordId={}, audioUrl={}, isFromUserUpload={}", 
                    request.getNickname(), request.getRecordId(), response.getAudioUrl(), response.getIsFromUserUpload());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // 클라이언트 요청 오류 (400 Bad Request)
            log.warn("잘못된 오디오 요청: nickname={}, recordId={}, error={}", 
                    request.getNickname(), request.getRecordId(), e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (RuntimeException e) {
            // 비즈니스 로직 오류 또는 서버 내부 오류
            log.error("오디오 조회/생성 API 오류: nickname={}, recordId={}, error={}", 
                    request.getNickname(), request.getRecordId(), e.getMessage(), e);
            
            // 구체적인 오류 메시지에 따른 HTTP 상태 코드 결정
            if (e.getMessage().contains("찾을 수 없습니다") || e.getMessage().contains("없습니다")) {
                return ResponseEntity.notFound().build(); // 404 Not Found
            } else if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(403).build(); // 403 Forbidden
            } else {
                return ResponseEntity.internalServerError().build(); // 500 Internal Server Error
            }
        }
    }

    /**
     * 사용 가능한 음성 타입 목록 조회
     * 
     * TTS 생성 시 선택할 수 있는 모든 음성 타입을 반환합니다.
     * 클라이언트에서 UI 구성이나 유효성 검증에 활용할 수 있습니다.
     * 
     * 응답 구조:
     * - voiceKey: 시스템 내부 음성 키
     * - voiceName: 사용자 친화적인 음성 이름
     * 
     * 예시 응답:
     * [
     *   {"voiceKey": "vara", "voiceName": "아라"},
     *   {"voiceKey": "vmikyung", "voiceName": "미경"},
     *   ...
     * ]
     * 
     * @return 음성 타입 목록
     */
    @Operation(
            summary = "음성 타입 목록 조회",
            description = "TTS 생성에 사용할 수 있는 모든 음성 타입 목록을 반환"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공적으로 음성 타입 목록을 반환",
                    content = @Content(schema = @Schema(implementation = VoiceTypeResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/voice/types")
    public ResponseEntity<List<VoiceTypeResponse>> getVoiceTypes() {
        log.debug("음성 타입 목록 조회 API 호출");
        
        try {
            // VoiceType enum의 모든 값을 응답 DTO로 변환
            List<VoiceTypeResponse> voiceTypes = Arrays.stream(VoiceType.values())
                    .map(voiceType -> new VoiceTypeResponse(
                            voiceType.name(),           // enum 이름 (ARA, MIKYUNG 등)
                            voiceType.getVoiceKey(),    // 시스템 키 (vara, vmikyung 등)
                            voiceType.getVoiceName()    // 표시명 (아라, 미경 등)
                    ))
                    .collect(Collectors.toList());
            
            log.debug("음성 타입 목록 조회 완료: count={}", voiceTypes.size());
            return ResponseEntity.ok(voiceTypes);
            
        } catch (Exception e) {
            log.error("음성 타입 목록 조회 중 오류 발생: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 음성 타입 응답 DTO
     * 
     * 클라이언트에게 음성 타입 정보를 전달하기 위한 응답 객체입니다.
     * VoiceType enum의 정보를 클라이언트 친화적인 형태로 변환합니다.
     */
    @Schema(description = "음성 타입 정보")
    public static class VoiceTypeResponse {
        /**
         * enum 이름 (예: ARA, MIKYUNG)
         * 클라이언트에서 요청 시 사용할 값
         */
        @Schema(description = "음성 타입 enum 이름", example = "ARA")
        private final String name;
        
        /**
         * 시스템 내부 음성 키 (예: vara, vmikyung)
         * TTS 서비스 호출 시 사용되는 실제 키
         */
        @Schema(description = "시스템 내부 음성 키", example = "vara")
        private final String voiceKey;
        
        /**
         * 사용자 친화적인 음성 이름 (예: 아라, 미경)
         * UI에서 사용자에게 표시할 이름
         */
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

    /**
     * 오디오 API 헬스 체크
     * 
     * 오디오 관련 서비스의 상태를 확인하는 간단한 헬스 체크 엔드포인트입니다.
     * 모니터링이나 로드 밸런서에서 서비스 가용성을 확인하는 데 사용할 수 있습니다.
     * 
     * @return 서비스 상태 정보
     */
    @GetMapping("/audio/health")
    public ResponseEntity<String> healthCheck() {
        log.debug("오디오 API 헬스 체크 호출");
        
        try {
            // 기본적인 서비스 가용성 확인
            // 실제 운영 환경에서는 더 상세한 헬스 체크 로직 추가 가능
            // 예: DB 연결 상태, S3 연결 상태, TTS 서비스 상태 등
            
            return ResponseEntity.ok("Audio API is healthy");
            
        } catch (Exception e) {
            log.error("오디오 API 헬스 체크 실패: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Audio API is unhealthy");
        }
    }
}