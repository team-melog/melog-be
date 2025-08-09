package com.melog.melog.clova.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.in.AnalyzeSentimentUseCase;
import com.melog.melog.clova.application.port.out.ExtractEmotionPort;
import com.melog.melog.clova.domain.model.MessangerType;
import com.melog.melog.clova.domain.model.PromptMessage;
import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.request.ExtractEmotionRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;
import com.melog.melog.clova.domain.model.response.ExtractEmotionResponse;
import com.melog.melog.emotion.domain.EmotionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeSentimentService implements AnalyzeSentimentUseCase {

    private final ExtractEmotionPort clovaStudioPort;
    private final ObjectMapper objectMapper;

    @Override
    public AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request) {
        final String nickname = request.getNickname();
        final String text = request.getText();

        List<PromptMessage> prompt = new ArrayList<>(buildPromptPrefix(objectMapper));
        prompt.add(new PromptMessage(MessangerType.USER, text));

        log.info("{}의 감정 분석 요청:", nickname);
        prompt.forEach(pm -> log.info(pm.toPrompt()));

        ExtractEmotionRequest chatRequest = ExtractEmotionRequest.builder()
                .nickname(nickname)
                .promptMessages(prompt)
                .build();

        ExtractEmotionResponse studioRes = clovaStudioPort.sendRequest(chatRequest);

        return AnalyzeSentimentResponse.builder()
                .emotionResults(studioRes.getEmotionResults())
                .summary(studioRes.getSummary())
                .keywords(studioRes.getKeywords())
                .build();
    }

    /** ----- 프롬프트/샘플 관련 유틸 ----- */
    public static ExtractEmotionResponse buildSampleResponse() {
        int size = EmotionType.values().length;
        int basePct = 100 / size;
        int remainder = 100 % size;

        List<ExtractEmotionResponse.EmotionResult> results = new ArrayList<>();
        int idx = 0;
        for (EmotionType type : EmotionType.values()) {
            int pct = basePct + (idx < remainder ? 1 : 0);
            results.add(new ExtractEmotionResponse.EmotionResult(type, pct));
            idx++;
        }

        return ExtractEmotionResponse.builder()
                .emotionResults(results)
                .build();
    }

    private static String buildSampleJsonString(ObjectMapper om) {
        try {
            EmotionType[] values = EmotionType.values();
            int size = values.length;
            int basePct = 100 / size;
            int remainder = 100 % size;

            List<ExtractEmotionResponse.EmotionResult> sampleList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int pct = basePct + (i < remainder ? 1 : 0);
                sampleList.add(new ExtractEmotionResponse.EmotionResult(values[i], pct));
            }

            ExtractEmotionResponse sample = ExtractEmotionResponse.builder()
                    .emotionResults(sampleList)
                    .summary("오늘 아경님의 목소리에는 지침(50%), 분노(30%), 평온(20%)이 섞여 있었습니다. 업무량이 많아 몸과 마음이 무겁지만, 일 자체에는 여전히 흥미를 느끼고 있는 상태예요. 다만 주변 동료와의 관계나 환경에서 오는 스트레스가 피로감을 키우고 있어요. 이런 상황에서는 잠깐의 휴식이나 가벼운 대화로 긴장을 풀어주는 것이 도움이 될 수 있습니다. 당신의 열정은 여전히 살아있으니, 에너지를 회복할 시간을 꼭 챙겨주세요.")
                    .keywords(List.of("피로", "스트레스", "업무", "열정", "휴식"))
                    .build();

            return om.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(sample);

        } catch (JsonProcessingException e) {
            return """
                    {
                      "emotionResults": [
                        {"emotion": "JOY", "percentage": 50},
                        {"emotion": "SADNESS", "percentage": 50}
                      ],
                      "summary": "오늘 목소리에서 기쁨(50%)과 슬픔(50%)이 혼재되어 있었습니다. 좋은 일이 있었지만 동시에 아쉬운 부분도 있어 복합적인 감정을 느끼고 있는 상태로 보입니다.",
                      "keywords": ["키워드1", "키워드2", "키워드3", "키워드4", "키워드5"]
                    }
                    """;
        }
    }

    private static List<PromptMessage> buildPromptPrefix(ObjectMapper om) {
        String emotionsEnglish = String.join(", ",
                Arrays.stream(EmotionType.values()).map(Enum::name).toList());

        String systemContent = String.join("\n",
                "You are an API that analyzes the user's emotion from text.",
                "⚠️ IMPORTANT: The text may contain STT (Speech-to-Text) conversion errors.",
                "Read the text contextually and infer the intended meaning even if there are typos or word conversion errors.",
                "Focus on the overall emotional context and meaning rather than individual word accuracy.",
                "Classify the emotion into the following 6 categories: " + emotionsEnglish + ".",
                "Return ONLY a raw JSON object with the following structure:",
                "- 'emotionResults': array of emotion objects with 'emotion' (uppercase) and 'percentage' (integer)",
                "- 'summary': detailed emotional analysis including emotion percentages and reasoning within 300 characters",
                "- 'keywords': array of exactly 5 key words in order of importance (correct the words if STT errors are detected)",
                "The sum of all emotion percentages MUST equal exactly 100.",
                "The emotion must be one of: " + emotionsEnglish + ".",
                "⚠️ CRITICAL: You MUST respond with the exact JSON format shown below.",
                "⚠️ DO NOT use markdown formatting. DO NOT wrap in backticks.",
                "⚠️ DO NOT include any commentary or explanation. Pure JSON only.",
                "⚠️ If response format is incorrect, it will be treated as an error.",
                "Example response:",
                buildSampleJsonString(om)
        );

        return List.of(new PromptMessage(MessangerType.SYSTEM, systemContent));
    }
}