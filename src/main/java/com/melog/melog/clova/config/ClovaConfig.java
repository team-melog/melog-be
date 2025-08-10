// src/main/java/com/melog/melog/clova/config/ClovaConfig.java
package com.melog.melog.clova.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "clova")
public class ClovaConfig {
    private SpeechProps speech;
    private StudioProps studio;

    @PostConstruct
    public void logConfig() {
        log.info("ClovaConfig loaded - speech: {}, studio: {}", speech, studio);
        if (speech != null) {
            log.info("Speech URL: {}, Client ID: {}, Client Secret: {}", 
                    speech.getUrl(), speech.getClientId(), speech.getClientSecret());
        }
        if (studio != null) {
            log.info("Studio Base URL: {}, API Key: {}", studio.getBaseUrl(), studio.getApiKey());
        }
    }

    @Getter @Setter
    public static class SpeechProps {
        private String url;
        private String clientId;
        private String clientSecret;
        private SttProps stt;
        private int timeoutMs = 10000;
    }

    @Getter @Setter
    public static class SttProps {
        private String endpoint;
        private String defaultLang;
        private int maxDuration;
        private String[] supportedFormats;
    }

    @Getter @Setter
    public static class StudioProps {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int timeoutMs = 8000;
    }
}
