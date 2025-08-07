package com.melog.melog.clova.application.service;

import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.ClovaStudioChatRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ClovaStudioChatResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyzeSentimentService implements AnalyzeSentimentUseCase {

    private final ClovaStudioPort clovaStudioPort;

    @Override
    public AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request) {
        // 감정 분석을 위한 프롬프트 구성
        String prompt = String.format(
            "다음 텍스트의 감정을 분석해주세요. 긍정, 부정, 중립 중 하나로 분류하고, 신뢰도를 백분율로 표시해주세요.\n\n텍스트: %s",
            request.getText()
        );

        ClovaStudioChatRequest chatRequest = ClovaStudioChatRequest.builder()
                .messages(Arrays.asList(
                    ClovaStudioChatRequest.Message.builder()
                        .role("user")
                        .content(prompt)
                        .build()
                ))
                .maxTokens(100)
                .temperature(0.3)
                .build();

        ClovaStudioChatResponse response = clovaStudioPort.sendChatRequest(chatRequest);

        // 응답에서 감정 분석 결과 추출
        String result = response.getChoices().get(0).getMessage().getContent();

        return AnalyzeSentimentResponse.builder()
                .result(result)
                .build();
    }
}
