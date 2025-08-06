package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AnalyzeSentimentService implements AnalyzeSentimentUseCase {

    private final ClovaStudioPort clovaApiPort;

    @Override
    public AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request) {

        // Studio 스크립트 작성해야함.
        clovaApiPort.sendRequest(
                ClovaStudioRequest.builder()
                        .build());
        // 감정 분석 저장.

        return AnalyzeSentimentResponse.builder().result("결과").build();
    }

}
