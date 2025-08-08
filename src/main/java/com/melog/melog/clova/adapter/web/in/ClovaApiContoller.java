package com.melog.melog.clova.adapter.web.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.in.SpeechSttUseCase;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.SpeechToTextRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.SpeechToTextResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clova")
@RequiredArgsConstructor
public class ClovaApiContoller {

    private final AnalyzeSentimentUseCase analyzeSentimentUseCase;
    private final SpeechSttUseCase speechSttUseCase;
    
    @PostMapping("/analyze")
    public AnalyzeSentimentResponse analyzeSentiment(
        @RequestBody AnalyzeSentimentRequest request
    ) {
        return analyzeSentimentUseCase.execute(request);
    }

    @PostMapping("/speech")
    public SpeechToTextResponse speechToText(
        @RequestBody SpeechToTextRequest request
    ) {
        return speechSttUseCase.execute(request);
    }



    
}