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

    // === 교체 1: 기존 메서드 전체 대체 ===
    private static String buildSampleJsonString(ObjectMapper om) {
        // 예시 JSON은 의도된 스펙(한글 type, 상위3개, 키워드 3개, 600~800자)에 맞춥니다.
        return """
        {
        "emotionResults": [
            {"type": "기쁨", "percentage": 60},
            {"type": "설렘", "percentage": 25},
            {"type": "평온", "percentage": 15}
        ],
        "summary": "오늘 하루, 기쁨이 가장 크게 느껴졌어요. 그 안에 설렘과 평온도 함께 있었네요. 라면을 끓여 소세지와 곁들여 드시며 작은 행복을 느끼셨죠. 문어 육수 라면이 없어 아쉬움이 남았지만, 그럼에도 스스로를 챙기며 시간을 잘 보내셨네요. 오늘만큼은 느껴진 감정을 있는 그대로 두셔도 괜찮아요. 작은 즐거움이 쌓이면 하루의 분위기도 달라져요. 잠깐 쉬어가며 몸과 마음을 돌보면 내일은 더 가벼운 마음으로 일상을 맞이하실 수 있을 거예요.",
        "keywords": ["행복", "기대", "편안함"]
        }
        """;
    }

    // === 교체 2: 기존 메서드 전체 대체 ===
    private static List<PromptMessage> buildPromptPrefix(ObjectMapper om) {
        String systemContent = String.join("\n",
            "당신은 감정 분석 전문가이자 따뜻한 상담가입니다.",
            "반드시 '사용자에게 직접 말하는 1:1 대화체'로 피드백을 제공합니다.",
            "제3자 시점(예: \"그/그녀는 ~했다\"), 동화체/나레이터식 서술, 마크다운(코드블록 포함), 이모지는 금지합니다.",
            "",
            "출력은 오직 다음 형태의 JSON 객체 하나만 반환하세요:",
            "- emotionResults: 상위 3개 감정 배열 [{\"type\":<한글 감정명>, \"percentage\":<정수>}] (합계 100)",
            "- summary: 600~800자, 아래 3단계 대화 형식으로 작성",
            "- keywords: 정의된 키워드 풀에서만 선택한 3개(중요도 순)",
            "",
            "[감정 종류(한글)] 기쁨, 설렘, 평온, 분노, 슬픔, 지침",
            "",
            "[summary 3단계 대화 형식(모두 2인칭 구어체)]",
            "1) 감정 멘트: 상위 감정(1순위 중심, 2~3순위 자연스럽게 반영) 기반 공감",
            "2) 오늘 돌아보기: 사건 나열 X, 감정 흐름 중심(예: \"~하셨군요\", \"~느껴졌네요\")",
            "3) 위로와 조언: 따뜻하고 현실적인 한마디로 마무리",
            "- 공격적/판단적 표현 금지, 의료/심리 치료 직접 언급 금지",
            "",
            "[키워드 풀]",
            "기쁨: 행복, 즐거움, 만족, 신남",
            "설렘: 기대, 떨림, 긴장, 두근거림",
            "평온: 차분함, 여유, 안정, 편안함",
            "분노: 화남, 짜증, 열받음, 격분",
            "슬픔: 슬픔, 우울함, 절망, 허전함",
            "지침: 혼란, 막막함, 고민, 갈등",
            "",
            "[형식 규칙(엄수)]",
            "⚠️ JSON 이외의 어떠한 문장도 출력하지 마세요.",
            "⚠️ 마크다운/설명문/접두문 금지.",
            "예시 응답:",
            buildSampleJsonString(om)
        );

        return List.of(new PromptMessage(MessangerType.SYSTEM, systemContent));
    }    
}