package com.melog.melog.clova.domain.model.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyzeSentimentRequest {
    private String nickname;
    private String text;
}
