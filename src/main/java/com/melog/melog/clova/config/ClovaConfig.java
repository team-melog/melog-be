// src/main/java/com/melog/melog/clova/config/ClovaConfig.java
package com.melog.melog.clova.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@Getter
public class ClovaConfig {
    
    // Speech API 설정
    @Value("${CLOVA_SPEECH_URL:https://naveropenapi.apigw.ntruss.com}")
    private String speechUrl;
    
    @Value("${CLOVA_SPEECH_CLIENT_ID}")
    private String speechClientId;
    
    @Value("${CLOVA_SPEECH_CLIENT_SECRET}")
    private String speechClientSecret;
    
    @Value("${CLOVA_SPEECH_STT_ENDPOINT:/recog/v1/stt}")
    private String speechSttEndpoint;
    
    @Value("${CLOVA_SPEECH_DEFAULT_LANG:Kor}")
    private String speechDefaultLang;
    
    @Value("${CLOVA_SPEECH_MAX_DURATION:60}")
    private int speechMaxDuration;
    
    @Value("${CLOVA_SPEECH_SUPPORTED_FORMATS:mp3,aac,ac3,ogg,flac,wav}")
    private String speechSupportedFormats;
    
    @Value("${CLOVA_SPEECH_TIMEOUT_MS:10000}")
    private int speechTimeoutMs;
    
    // Studio API 설정
    @Value("${CLOVA_STUDIO_BASE_URL:https://clovastudio.stream.ntruss.com}")
    private String studioBaseUrl;
    
    @Value("${CLOVA_STUDIO_API_KEY}")
    private String studioApiKey;
    
    @Value("${CLOVA_STUDIO_MODEL:HCX-005}")
    private String studioModel;
    
    @Value("${CLOVA_STUDIO_TIMEOUT_MS:8000}")
    private int studioTimeoutMs;

    @PostConstruct
    public void logConfig() {
        log.info("ClovaConfig loaded from environment variables");
        log.info("Speech URL: {}, Client ID: {}, Client Secret: {}", 
                speechUrl, speechClientId, speechClientSecret);
        log.info("Studio Base URL: {}, API Key: {}, Model: {}", 
                studioBaseUrl, studioApiKey, studioModel);
    }

    // 기존 코드와의 호환성을 위한 getter 메서드들
    public SpeechProps getSpeech() {
        return new SpeechProps();
    }
    
    public StudioProps getStudio() {
        return new StudioProps();
    }

    @Getter
    public class SpeechProps {
        public String getUrl() { return speechUrl; }
        public String getClientId() { return speechClientId; }
        public String getClientSecret() { return speechClientSecret; }
        public SttProps getStt() { return new SttProps(); }
        public int getTimeoutMs() { return speechTimeoutMs; }
    }

    @Getter
    public class SttProps {
        public String getEndpoint() { return speechSttEndpoint; }
        public String getDefaultLang() { return speechDefaultLang; }
        public int getMaxDuration() { return speechMaxDuration; }
        public String[] getSupportedFormats() { 
            return speechSupportedFormats.split(","); 
        }
    }

    @Getter
    public class StudioProps {
        public String getBaseUrl() { return studioBaseUrl; }
        public String getApiKey() { return studioApiKey; }
        public String getModel() { return studioModel; }
        public int getTimeoutMs() { return studioTimeoutMs; }
    }
}
