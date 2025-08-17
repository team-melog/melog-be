package com.melog.melog.clova.adapter.external.out;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.out.TextToSpeakPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.ClovaAppProps;
import com.melog.melog.clova.config.ClovaConfig.TtsProps;
import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.clova.domain.model.request.TtsApiRequest;
import com.melog.melog.clova.domain.model.response.TtsApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextToSpeakAdapter implements TextToSpeakPort {

    private final RestTemplate restTemplate;
    private final ClovaConfig clovaConfig;

    @Override
    public TtsApiResponse sendTextToSpeakRequest(TtsApiRequest request) {
        final ClovaAppProps clovaAppProps = clovaConfig.getClovaApp();
        final String baseUrl = clovaAppProps.getUrl();
        final TtsProps ttsProps = clovaAppProps.getTts();

        final String endpoint = ttsProps.getEndpoint();
        final String requestUrl = baseUrl + endpoint;    
        final String format = "wav";                              

        // 요청 파라미터
        final String text = request.getText();
        final VoiceType voiceType=  request.getVoiceType();
        final String speaker = voiceType.getVoiceKey();

        // 헤더
        HttpHeaders headers = buildHeaders(clovaAppProps.getClientId(), clovaAppProps.getClientSecret());

        // 폼 파라미터 조립 (인코딩 필수)
        Map<String, String> params = new LinkedHashMap<>();
        params.put("speaker", encode(speaker));
        // params.put("volume", String.valueOf(volume));
        // params.put("speed", String.valueOf(speed));
        // params.put("pitch", String.valueOf(pitch));
        params.put("format", format);
        params.put("text", encode(text));
        params.put("emotion", String.valueOf(request.getEmotion()));
        params.put("emotion-strength", String.valueOf(request.getEmotionStrength()));

        StringJoiner sj = new StringJoiner("&");
        params.forEach((k, v) -> sj.add(k + "=" + v));
        String formBody = sj.toString();

        HttpEntity<String> entity = new HttpEntity<>(formBody, headers);

        log.info("\n[CLOVA TTS] POST {}\n  headers: {}\n  body(form): {}",
                requestUrl, headers.toSingleValueMap(), formBody.replaceAll("text=[^&]+", "text=<omitted>"));

        try {
            // 응답은 바이너리 오디오
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, byte[].class);

            MediaType ct = response.getHeaders().getContentType();
            byte[] audio = response.getBody();
            int size = (audio == null ? 0 : audio.length);

            log.info("[CLOVA TTS] status={} contentType={} bytes={}",
                    response.getStatusCodeValue(), ct, size);

            return TtsApiResponse.builder()
                    .audioByteArr(audio)
                    .audioFileSize(size)
                    .voiceType(voiceType)
                    .format(format)
                    .build();

        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("[CLOVA TTS] API error {} body={}", e.getStatusCode(), body, e);
            throw e;
        } catch (Exception e) {
            log.error("[CLOVA TTS] request failed", e);
            throw e;
        }
    }

    private HttpHeaders buildHeaders(String clovaAppClientId, String clovaAppClientSecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-NCP-APIGW-API-KEY-ID", clovaAppClientId);
        headers.set("X-NCP-APIGW-API-KEY", clovaAppClientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.ACCEPT, "*/*");
        return headers;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}