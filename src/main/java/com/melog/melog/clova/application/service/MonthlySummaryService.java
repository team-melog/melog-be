package com.melog.melog.clova.application.service;

import com.melog.melog.clova.application.port.in.MonthlySummaryUseCase;
import com.melog.melog.clova.domain.model.request.MonthlySummaryRequest;
import com.melog.melog.clova.domain.model.response.MonthlySummaryResponse;
import com.melog.melog.clova.adapter.external.out.ClovaStudioAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlySummaryService implements MonthlySummaryUseCase {

    private final ClovaStudioAdapter clovaStudioAdapter;

    @Override
    public MonthlySummaryResponse generateMonthlySummary(MonthlySummaryRequest request) {
        try {
            log.info("월별 요약 생성 시작 - 월: {}, 감정 분포: {}", request.getMonth(), request.getEmotionDistribution());
            
            // 감정 분포 데이터가 있는지 확인
            if (request.getEmotionDistribution() == null || request.getEmotionDistribution().isEmpty()) {
                return MonthlySummaryResponse.builder()
                        .summary("이번 달에는 분석할 감정 기록이 없습니다.")
                        .advice("다음 달에는 더 많은 감정을 기록해보세요.")
                        .build();
            }

            // Clova Studio에 감정 분포 기반 요약 요청
            String prompt = buildEmotionDistributionPrompt(request.getMonth(), request.getEmotionDistribution());
            String clovaResponse = clovaStudioAdapter.generateText(prompt);
            
            // 응답 파싱
            MonthlySummaryResponse response = parseClovaResponse(clovaResponse);
            
            log.info("월별 요약 생성 완료 - 요약 길이: {}", response.getSummary().length());
            
            return response;
            
        } catch (Exception e) {
            log.error("월별 요약 생성 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시 기본 응답 반환
            return MonthlySummaryResponse.builder()
                    .summary("감정 요약을 생성하는 중 오류가 발생했습니다.")
                    .advice("잠시 후 다시 시도해주세요.")
                    .build();
        }
    }

    /**
     * Clova Studio에 전달할 프롬프트를 생성합니다.
     */
    private String buildSummaryPrompt(String month, String combinedComments) {
        return String.format("""
            %s월 감정 기록을 분석해주세요.
            
            감정 기록: %s
            
            응답 형식 (간결하게):
            요약: [2-3줄로 간단히]
            조언: [2-3줄로 간단히]
            
            주의: 응답은 300자 이내로 간결하게 작성해주세요.
            """, month, combinedComments);
    }

    /**
     * 감정 분포 데이터를 기반으로 Clova Studio에 전달할 프롬프트를 생성합니다.
     */
    private String buildEmotionDistributionPrompt(String month, Map<String, Double> emotionDistribution) {
        StringBuilder distributionText = new StringBuilder();
        for (Map.Entry<String, Double> entry : emotionDistribution.entrySet()) {
            if (entry.getValue() > 0) {
                distributionText.append(String.format("%s: %.1f%%, ", entry.getKey(), entry.getValue()));
            }
        }
        
        return String.format("""
            %s월 감정 분포를 분석해주세요.
            
            감정 분포: %s
            
            응답 형식 (간결하게):
            요약: [2-3줄로 간단히]
            조언: [2-3줄로 간단히]
            
            주의: 응답은 300자 이내로 간결하게 작성해주세요.
            """, month, distributionText.toString().replaceAll(", $", ""));
    }

    /**
     * Clova Studio 응답을 파싱합니다.
     */
    private MonthlySummaryResponse parseClovaResponse(String clovaResponse) {
        String summary = "";
        String advice = "";

        try {
            String[] lines = clovaResponse.split("\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                if (trimmedLine.startsWith("요약:")) {
                    summary = trimmedLine.substring(3).trim();
                } else if (trimmedLine.startsWith("조언:")) {
                    advice = trimmedLine.substring(3).trim();
                }
            }
        } catch (Exception e) {
            log.warn("Clova Studio 응답 파싱 중 오류 발생: {}", e.getMessage());
        }

        // 기본값 설정
        if (summary.isEmpty()) {
            summary = "이번 달의 감정 기록을 분석한 결과입니다.";
        }
        
        if (advice.isEmpty()) {
            advice = "감정을 기록하는 습관을 계속 유지해보세요.";
        }

        return MonthlySummaryResponse.builder()
                .summary(summary)
                .advice(advice)
                .build();
    }

} 
