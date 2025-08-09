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
    private String audioData; // Base64 인코딩된 오디오 데이터 (deprecated)
    private byte[] audioBinary; // Raw binary audio data for CSR API
    private String audioFormat; // "wav", "mp3", "m4a" 등
    private String language; // "ko", "en", "ja", "zh" 등
    private Boolean diarization; // 화자 분리 사용 여부
    private String diarizationConfig; // 화자 분리 설정
    private Boolean boost; // 인식 정확도 향상 옵션
    private String boostConfig; // 인식 정확도 향상 설정
}
