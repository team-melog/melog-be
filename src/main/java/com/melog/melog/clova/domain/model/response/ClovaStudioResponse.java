package com.melog.melog.clova.domain.model.response;

import lombok.Getter;

@Getter
public class ClovaStudioResponse implements ClovaApiResponse {
    
    private String text;
    private String language;

}
