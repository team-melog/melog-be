package com.melog.melog.clova.domain.model.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResponse {
    private String text; // 인식한 음원의 결과 값
    private Integer quota; // 음원 길이(초)
    private Integer assessmentScore; // 문장 전체의 발음 점수 (1~100)
    private String assessmentDetails; // 매 단어마다의 평가 점수
    private List<Integer> refGraph; // 기준 발음에 대한 음성 파형 그래프 수치 값
    private List<Integer> usrGraph; // 입력된 발음에 대한 음성 파형 그래프 수치 값
    private String language; // 인식된 언어
}
