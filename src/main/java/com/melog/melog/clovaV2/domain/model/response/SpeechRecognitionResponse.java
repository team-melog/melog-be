package com.melog.melog.clovaV2.domain.model.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionResponse {
    private String text;
    private List<Segment> segments;
    private Double confidence;
    private String language;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String text;
        private Double start;
        private Double end;
        private Double confidence;
        private Integer speakerId; // 화자 분리 사용 시
    }
}
