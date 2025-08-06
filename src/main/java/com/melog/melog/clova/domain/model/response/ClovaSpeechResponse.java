package com.melog.melog.clova.domain.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClovaSpeechResponse implements ClovaApiResponse {

    private String speechText;
    private String audioUrl;
    
}
