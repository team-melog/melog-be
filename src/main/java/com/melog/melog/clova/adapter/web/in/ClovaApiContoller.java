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
import com.melog.melog.clova.application.port.in.SpeechToTextUseCase;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.SttResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/clova")
@RequiredArgsConstructor
public class ClovaApiContoller {

    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechToTextUseCase speechSttUseCase;
    
    /**
     * 감정 분석 API
     * POST /api/clova/analyze
     */
    @PostMapping("/analyze")
    public AnalyzeSentimentResponse analyzeSentiment(
        @RequestBody AnalyzeSentimentRequest request
    ) {
        return analyzeSentimentUseCase.execute(request);
    }

    /**
     * 음성 인식 API (CLOVA Speech STT)
     * POST /api/clova/speech
     * 
     * @param audio 오디오 파일 (MP3, AAC, AC3, OGG, FLAC, WAV 형식, 60초 이내)
     * @param language 언어 설정 (ko, en, ja, zh - 기본값: ko)
     * @return STT 응답 (인식된 텍스트, 발음 평가, 음성 파형 그래프 등)
     */
    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SttResponse> recognizeSpeech(
        @RequestParam("audio") MultipartFile audio,
        @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        log.info("Received STT request: filename={}, size={}bytes, language={}", 
                audio.getOriginalFilename(), audio.getSize(), language);
        
        try {
            SttResponse response = speechSttUseCase.recognize(audio, language);
            log.info("STT completed successfully: text={}", response.getText());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("STT failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}