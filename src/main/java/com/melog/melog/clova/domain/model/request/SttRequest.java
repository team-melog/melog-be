package com.melog.melog.clova.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttRequest {
    private byte[] audioBinary; // Raw binary audio data
    private String audioFormat; // "mp3", "aac", "ac3", "ogg", "flac", "wav"
    private String language; // "Kor", "Eng", "Jpn", "Chn"
    private Boolean assessment; // 발음 평가 결과 반환 여부 (Kor, Eng만 지원)
    private String utterance; // 발음 평가 대상 텍스트
    private String boostings; // 음성 인식률 향상 키워드 (탭으로 구분, 한국어만 지원)
    private Boolean graph; // 음성 파형 그래프 반환 여부
}
