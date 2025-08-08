package com.melog.melog.clova.application.service;

import java.util.ArrayList;
import java.util.Arrays;
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

        List<PromptMessage> prompt = new ArrayList<>(buildPromptPrefix(objectMapper));
        prompt.add(new PromptMessage(MessangerType.USER, text));

        log.info("{}의 감정 분석 요청:", nickname);
        prompt.forEach(pm -> log.info(pm.toPrompt()));

        ClovaStudioRequest chatRequest = ClovaStudioRequest.builder()
                .nickname(nickname)
                .promptMessages(prompt)
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
            int pct = basePct + (idx < remainder ? 1 : 0);
            results.add(new ClovaStudioResponse.EmotionResult(type, pct));
            idx++;
        }

        return ClovaStudioResponse.builder()
                .emotionResults(results)
                .build();
    }

    private static String buildSampleJsonString(ObjectMapper om) {
        try {
            EmotionType[] values = EmotionType.values();
            int size = values.length;
            int basePct = 100 / size;
            int remainder = 100 % size;

            List<ClovaStudioResponse.EmotionResult> sampleList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int pct = basePct + (i < remainder ? 1 : 0);
                sampleList.add(new ClovaStudioResponse.EmotionResult(values[i], pct));
            }

            ClovaStudioResponse sample = ClovaStudioResponse.builder()
                    .emotionResults(sampleList)
                    .build();

            return om.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(sample);

        } catch (JsonProcessingException e) {
            return """
                    {
                      "emotionResults": [
                        {"emotion": "JOY", "percentage": 50},
                        {"emotion": "SADNESS", "percentage": 50}
                      ]
                    }
                    """;
        }
    }

    private static List<PromptMessage> buildPromptPrefix(ObjectMapper om) {
        String emotionsEnglish = String.join(", ",
                Arrays.stream(EmotionType.values()).map(Enum::name).toList());

        String systemContent = String.join("\n",
                "You are an API that analyzes the user's emotion from text.",
                "Classify the emotion into the following 6 categories: " + emotionsEnglish + ".",
                "Return ONLY a raw JSON object with a key 'emotionResults'.",
                "Each item must include an 'emotion' (uppercase string) and its 'percentage' (integer).",
                "The sum of all percentages MUST equal exactly 100.",
                "⚠️ DO NOT use markdown formatting. DO NOT wrap the JSON in triple backticks or label it with 'json'.",
                "DO NOT include any commentary or explanation. Respond with pure JSON only.",
                "Example response:",
                buildSampleJsonString(om) // This already returns pure JSON string
        );

        return List.of(new PromptMessage(MessangerType.SYSTEM, systemContent));
    }
}