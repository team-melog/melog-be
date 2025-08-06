package com.melog.melog.clova.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ClovaStudioResponse;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AnalyzeSentimentService implements AnalyzeSentimentUseCase {

    private final ClovaStudioPort clovaApiPort;

    @Override
    public AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request) {

        // Studio 스크립트 작성해야함.
        ClovaStudioResponse response = clovaApiPort.sendRequest(
                ClovaStudioRequest.builder()
                        .text(request.getText())
                        .build());

        return AnalyzeSentimentResponse.builder().result(response.getResult().toString()).build();
    }

}
