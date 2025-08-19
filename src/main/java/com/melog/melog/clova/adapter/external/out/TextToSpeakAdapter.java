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

import com.melog.melog.clova.application.port.out.TextToSpeakPort;
import com.melog.melog.clova.config.ClovaConfig;
import com.melog.melog.clova.config.ClovaConfig.ClovaAppProps;
import com.melog.melog.clova.config.ClovaConfig.TtsProps;
import com.melog.melog.clova.domain.model.VoiceToner;
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
        final VoiceType voiceType = request.getVoiceType();
        final VoiceToner toner = (request.getToner() != null)
                ? request.getToner()
                : VoiceToner.builder().build(); // 모든 필드 0 기본

        final String formBody = buildFormBody(voiceType, text, format, toner);
        final HttpHeaders headers = buildHeaders(clovaAppProps.getClientId(), clovaAppProps.getClientSecret());
        final HttpEntity<String> entity = new HttpEntity<>(formBody, headers);

        log.info("\n[CLOVA TTS] POST {}\n  headers: {}\n  body(form): {}",
                requestUrl,
                safeHeaders(headers),
                safeForm(formBody));

        try {
            // 응답은 바이너리 오디오
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    requestUrl, HttpMethod.POST, entity, byte[].class);

            MediaType ct = response.getHeaders().getContentType();
            byte[] audio = response.getBody();
            int size = (audio == null ? 0 : audio.length);

            // ===== 응답 로그 =====
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

    private static String buildFormBody(
            VoiceType voiceType,
            String text,
            String format,
            VoiceToner toner) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("speaker", encode(voiceType.getVoiceKey()));
        params.put("text", encode(text));
        params.put("format", format);

        params.put("volume", String.valueOf(toner.getVolume()));
        params.put("speed", String.valueOf(toner.getSpeed()));
        params.put("pitch", String.valueOf(toner.getPitch()));
        params.put("alpha", String.valueOf(toner.getAlpha()));
        params.put("emotion", String.valueOf(toner.getEmotion()));
        params.put("emotion-strength", String.valueOf(toner.getEmotionStrength()));

        StringJoiner sj = new StringJoiner("&");
        params.forEach((k, v) -> sj.add(k + "=" + v));
        return sj.toString();
    }

    /* ===================== 로깅 보조 유틸 ===================== */

    // 헤더 안전 출력: API 키 마스킹
    private static Map<String, String> safeHeaders(HttpHeaders headers) {
        Map<String, String> map = new LinkedHashMap<>(headers.toSingleValueMap());
        map.computeIfPresent("X-NCP-APIGW-API-KEY", (k, v) -> "****");
        map.computeIfPresent("X-NCP-APIGW-API-KEY-ID", (k, v) -> maskEnd(v));
        return map;
    }

    // 폼 바디 안전 출력: text 값 숨김(대소문자 무시, 순서 무관)
    private static String safeForm(String formBody) {
        if (formBody == null)
            return "";
        return formBody.replaceAll("(?i)(^|&)(text)=([^&]*)", "$1$2=<omitted>");
    }

    private static String maskEnd(String id) {
        if (id == null || id.length() < 4)
            return "****";
        return id.substring(0, id.length() - 4) + "****";
    }
}
