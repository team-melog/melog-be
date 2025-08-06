package com.melog.melog.clova.domain.model.request;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClovaStudioRequest implements ClovaApiRequest {
    
    private String text;
    private String language;
    
}
