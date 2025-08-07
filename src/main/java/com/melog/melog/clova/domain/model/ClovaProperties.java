package com.melog.melog.clova.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClovaProperties {
    private String url;
    private String apiKey;
    private String apiKeyId;
    private String serviceId; // Clova Studio 서비스 ID
    private String modelName; // 사용할 모델명 (예: clova-x1-5b, clova-x1-9b)
    private Integer maxTokens; // 최대 토큰 수
    private Double temperature; // 생성 다양성 (0.0 ~ 1.0)
}
