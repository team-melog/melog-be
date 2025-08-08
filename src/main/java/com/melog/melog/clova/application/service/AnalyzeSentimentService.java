package com.melog.melog.clova.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.out.ClovaStudioPort;
import com.melog.melog.clova.domain.model.MessangerType;
import com.melog.melog.clova.domain.model.PromptMessage;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ClovaStudioResponse;
import com.melog.melog.common.model.EmotionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeSentimentService implements AnalyzeSentimentUseCase {

    private final ClovaStudioPort clovaStudioPort;
    private final ObjectMapper objectMapper;

    @Override
    public AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request) {
        final String nickname = request.getNickname();
        final String text = request.getText();

        // 1) 프롬프트 prefix + USER 메시지
        List<PromptMessage> prompt = new ArrayList<>(buildPromptPrefix(objectMapper));
        prompt.add(new PromptMessage(MessangerType.USER, text));

        log.info("{}의 감정 분석 요청:", nickname);
        prompt.forEach(pm -> log.info(pm.toPrompt()));

        // 2) 어댑터로 전달
        ClovaStudioRequest chatRequest = ClovaStudioRequest.builder()
                .nickname(nickname)
                .promptMessages(prompt) // 포트/어댑터에서 벤더 형식으로 변환
                .build();

        ClovaStudioResponse studioRes = clovaStudioPort.sendRequest(chatRequest);

        return AnalyzeSentimentResponse.builder()
                .emotionResults(studioRes.getEmotionResults())
                .build();
    }

    /** ----- 프롬프트/샘플 관련 유틸 ----- */

    public static ClovaStudioResponse buildSampleResponse() {
        int size = EmotionType.values().length;
        int basePct = 100 / size;
        int remainder = 100 % size;

        List<ClovaStudioResponse.EmotionResult> results = new ArrayList<>();
        int idx = 0;
        for (EmotionType type : EmotionType.values()) {
            int pct = basePct + (idx < remainder ? 1 : 0); // 합 100 유지
            results.add(new ClovaStudioResponse.EmotionResult(type, pct));
            idx++;
        }
        return ClovaStudioResponse.builder()
                .emotionResults(results)
                .build();
    }

    private static String buildSampleJsonString(ObjectMapper om) {
        try {
            return om.writerWithDefaultPrettyPrinter()
                     .writeValueAsString(buildSampleResponse());
        } catch (JsonProcessingException e) {
            // 실패 시 최소한의 예시라도 제공
            return """
                   {"emotionResults":[{"emotion":"기쁨","percentage":50},{"emotion":"슬픔","percentage":50}]}
                   """;
        }
    }

    private static List<PromptMessage> buildPromptPrefix(ObjectMapper om) {
        String emotionsKorean = String.join(", ",
                // EmotionType 이 @JsonValue로 한글을 내보내지 않는다면 toDisplayName() 등으로 바꿔주세요
                List.of(EmotionType.values()).stream().map(Enum::name).toList()
        );

        return List.of(
            new PromptMessage(MessangerType.SYSTEM, "다음 사용자 텍스트의 감정을 이하 6가지 감정으로 분석하십시오."),
            // 사람이 읽을 수 있게 한글/영문 중 원하는 표기 선택
            new PromptMessage(MessangerType.SYSTEM, "감정 목록: " + emotionsKorean),
            new PromptMessage(MessangerType.SYSTEM,
                "반환 형식은 반드시 JSON만 출력하고, 각 감정의 percentage는 0~100 정수, 총합 100이 되도록 하세요."),
            new PromptMessage(MessangerType.SYSTEM, "설명/부연 금지."),
            new PromptMessage(MessangerType.SYSTEM, "예시 : " + buildSampleJsonString(om))
        );
    }
}
