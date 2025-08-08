package com.melog.melog.clova.domain.model.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SpeechToTextResponse{
    private String text;
    private String language;
}
