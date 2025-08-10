// src/main/java/com/melog/melog/clova/config/ClovaConfig.java
package com.melog.melog.clova.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "clova")
public class ClovaConfig {
    private SpeechProps speech;
    private StudioProps studio;

    @Getter @Setter
    public static class SpeechProps {
        private String baseUrl;
        private String apiKey;
        private String apiKeyId;
        private int timeoutMs = 10000;
    }

    @Getter @Setter
    public static class StudioProps {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int timeoutMs = 8000;
    }

}
