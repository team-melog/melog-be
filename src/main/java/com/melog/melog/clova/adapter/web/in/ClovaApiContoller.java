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

@RestController
@RequestMapping("/api/clova")
@RequiredArgsConstructor
public class ClovaApiContoller {

    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechToTextUseCase speechSttUseCase;
    
    @PostMapping("/analyze")
    public AnalyzeSentimentResponse analyzeSentiment(
        @RequestBody AnalyzeSentimentRequest request
    ) {
        return analyzeSentimentUseCase.execute(request);
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SttResponse> recognizeSpeech(
        @RequestParam("audio") MultipartFile audio,
        @RequestParam(value = "language", defaultValue = "ko") String language
    ) {
        SttResponse response = speechSttUseCase.recognize(audio, language);
        return ResponseEntity.ok(response);
    }
}