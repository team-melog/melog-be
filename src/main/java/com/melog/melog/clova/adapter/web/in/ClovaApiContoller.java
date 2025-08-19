package com.melog.melog.clova.adapter.web.in;

import java.io.IOException;
import java.io.OutputStream;

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
import com.melog.melog.clova.application.port.in.TextToSpeakUseCase;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.TtsRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.SttResponse;
import com.melog.melog.clova.domain.model.response.TtsResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/clova")
@RequiredArgsConstructor
public class ClovaApiContoller {

    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechToTextUseCase speechSttUseCase;
    private final TextToSpeakUseCase textToSpeakUseCase;

    /**
     * 감정 분석 API
     * POST /api/clova/analyze
     */
    @PostMapping("/analyze")
    public AnalyzeSentimentResponse analyzeSentiment(
            @RequestBody AnalyzeSentimentRequest request) {
        return analyzeSentimentUseCase.execute(request);
    }

    /**
     * 음성 인식 API (CLOVA Speech STT)
     * POST /api/clova/speech
     * 
     * @param audio    오디오 파일 (MP3, AAC, AC3, OGG, FLAC, WAV 형식, 60초 이내)
     * @param language 언어 설정 (ko, en, ja, zh - 기본값: ko)
     * @return STT 응답 (인식된 텍스트, 발음 평가, 음성 파형 그래프 등)
     */
    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SttResponse> recognizeSpeech(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "language", defaultValue = "ko") String language) {
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

    @PostMapping(value = "/tts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void tts(@RequestBody TtsRequest req, HttpServletResponse response) throws IOException {

        TtsResponse res = textToSpeakUseCase.textToSpeak(req);

        byte[] audio = res.getAudioByteArr();
        String format = (res.getFormat() == null) ? "mp3" : res.getFormat().toLowerCase();

        switch (format) {
            case "mp3" -> response.setContentType("audio/mpeg");
            case "wav" -> response.setContentType("audio/wav");
            default -> response.setContentType("application/octet-stream");
        }

        String filename = req.getVoiceType() + "_" + System.currentTimeMillis() + "." + format;
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        if (res.getAudioFileSize() > 0) {
            response.setContentLength(res.getAudioFileSize());
        }

        try (OutputStream os = response.getOutputStream()) {
            os.write(audio);
            os.flush();
        }
    }

}