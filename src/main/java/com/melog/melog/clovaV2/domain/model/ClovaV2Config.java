package com.melog.melog.clovaV2.domain.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "clova-v2")
@Getter
@Setter
public class ClovaV2Config {
    private Studio studio;
    private Speech speech;

    @Getter
    @Setter
    public static class Studio {
        private String baseUrl;
        private String apiKey;
        private String apiKeyId;
        private String serviceId;
        private String modelName;
        private Integer maxTokens;
        private Double temperature;
    }

    @Getter
    @Setter
    public static class Speech {
        private String baseUrl;
        private String apiKey;
        private String apiKeyId;
        private String serviceId;
    }
}
