package com.melog.melog.clova.adapter.web.in;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.in.SpeechSttUseCase;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clova")
@RequiredArgsConstructor
@Tag(name = "Clova API", description = "네이버 Clova AI 서비스 API")
public class ClovaApiContoller {

    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechSttUseCase speechSttUseCase;
    
    @PostMapping("/analyze")
    @Operation(summary = "감정 분석", description = "텍스트의 감정을 분석합니다")
    public AnalyzeSentimentResponse analyzeSentiment(
        @RequestBody AnalyzeSentimentRequest request
    ) {
        return analyzeSentimentUseCase.execute(request);
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "CSR 음성 인식", 
        description = "음성 파일을 업로드하여 텍스트로 변환합니다",
        responses = {
            @ApiResponse(responseCode = "200", description = "음성 인식 성공", 
                content = @Content(schema = @Schema(implementation = ClovaSttResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 크기 초과, 형식 오류 등)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
        }
    )
    public ResponseEntity<ClovaSttResponse> recognizeSpeech(
        @Parameter(description = "음성 파일 (mp3, wav, m4a, flac 지원, 최대 3MB, 60초)", required = true)
        @RequestParam("audio") MultipartFile audio,
        @Parameter(description = "언어 코드", example = "ko")
        @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        ClovaSttResponse response = speechSttUseCase.recognize(audio, language);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/speech/short-sentence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Clova Studio 단문 음성 인식", 
        description = "Clova Studio API를 사용하여 단문 음성 파일을 텍스트로 변환합니다",
        responses = {
            @ApiResponse(responseCode = "200", description = "음성 인식 성공", 
                content = @Content(schema = @Schema(implementation = ClovaSttResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 크기 초과, 형식 오류 등)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
        }
    )
    public ResponseEntity<ClovaSttResponse> recognizeShortSentenceSpeech(
        @Parameter(description = "단문 음성 파일 (wav, mp3 지원, 최대 3MB, 10초)", required = true)
        @RequestParam("audio") MultipartFile audio,
        @Parameter(description = "언어 코드", example = "ko")
        @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        ClovaSttResponse response = speechSttUseCase.recognizeShortSentence(audio, language);
        return ResponseEntity.ok(response);
    }
}