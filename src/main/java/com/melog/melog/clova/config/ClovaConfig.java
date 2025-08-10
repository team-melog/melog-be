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
            log.info("Speech STT Endpoint: {}, Default Lang: {}", 
                    speech.getStt().getEndpoint(), speech.getStt().getDefaultLang());
        }
        if (studio != null) {
            log.info("Studio Base URL: {}, API Key: {}, Model: {}", 
                    studio.getBaseUrl(), studio.getApiKey(), studio.getModel());
        }
    }

    @Getter @Setter
    public static class SpeechProps {
        private String url;
        private String clientId;        // YAML: client-id
        private String clientSecret;    // YAML: client-secret
        private SttProps stt;
        private int timeoutMs;          // YAML: timeout-ms
    }

    @Getter @Setter
    public static class SttProps {
        private String endpoint;
        private String defaultLang;     // YAML: default-lang
        private int maxDuration;        // YAML: max-duration
        private String[] supportedFormats; // YAML: supported-formats
    }

    @Getter @Setter
    public static class StudioProps {
        private String baseUrl;         // YAML: base-url
        private String apiKey;          // YAML: api-key
        private String model;
        private int timeoutMs;          // YAML: timeout-ms
    }
}
