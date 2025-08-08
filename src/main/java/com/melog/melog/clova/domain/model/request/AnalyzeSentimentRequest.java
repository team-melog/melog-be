package com.melog.melog.clova.domain.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeSentimentRequest {
    private String nickname;
    private String text;
}
