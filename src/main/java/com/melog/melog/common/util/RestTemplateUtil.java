package com.melog.melog.common.util;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestTemplateUtil {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateUtil.class);
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;

    private final RestTemplate restTemplate;

    /*
     * =========================
     * Convenience overloads
     * =========================
     */

    // GET /no headers, no params
    public <T> T send(String url, Class<T> responseType) {
        return send(url, HttpMethod.GET, null, responseType, null, null, null);
    }

    // Method만 지정
    public <T> T send(String url, HttpMethod method, Class<T> responseType) {
        return send(url, method, null, responseType, null, null, null);
    }

    // Method + Headers
    public <T> T send(String url, HttpMethod method, Class<T> responseType, HttpHeaders headers) {
        return send(url, method, null, responseType, headers, null, null);
    }

    // Method + Body
    public <T> T send(String url, HttpMethod method, Object body, Class<T> responseType) {
        return send(url, method, body, responseType, null, null, null);
    }

    // Method + Body + Headers
    public <T> T send(String url, HttpMethod method, Object body, Class<T> responseType, HttpHeaders headers) {
        return send(url, method, body, responseType, headers, null, null);
    }

    // Method + PathVariables
    public <T> T sendWithPath(String url, HttpMethod method, Class<T> responseType,
            Map<String, ?> pathVariables) {
        return send(url, method, null, responseType, null, pathVariables, null);
    }

    // Method + QueryParams
    public <T> T sendWithQuery(String url, HttpMethod method, Class<T> responseType,
            Map<String, ?> queryParams) {
        return send(url, method, null, responseType, null, null, queryParams);
    }

    // Method + Body + Path + Query + Headers (가장 일반형)
    public <T> T send(String url,
            HttpMethod method,
            Object body,
            Class<T> responseType,
            HttpHeaders headers,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams) {
        return sendRequestWithRetry(url, method, body, responseType, headers, pathVariables, queryParams,
                DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_DELAY_MS);
    }

    /*
     * =========================
     * Core with retry
     * =========================
     */

    private <T> T sendRequestWithRetry(String url,
            HttpMethod method,
            Object requestBody,
            Class<T> responseType,
            HttpHeaders headers,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams,
            int maxRetryAttempts,
            long retryDelayMs) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                return executeRequest(url, method, requestBody, responseType, headers, pathVariables, queryParams);
            } catch (RestApiException e) {
                lastException = e;
                attempt++;

                if (attempt < maxRetryAttempts) {
                    logger.warn("HTTP request failed (attempt {}/{}), retrying in {}ms - URL: {}, Error: {}",
                            attempt, maxRetryAttempts, retryDelayMs, url, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RestApiException("Request interrupted", ie);
                    }
                }
            }
        }
        logger.error("HTTP request failed after {} attempts - URL: {}", maxRetryAttempts, url);
        throw new RestApiException("Failed to call API after " + maxRetryAttempts + " attempts: " + url, lastException);
    }

    /*
     * =========================
     * Execute
     * =========================
     */

    private <T> T executeRequest(String url,
            HttpMethod method,
            Object requestBody,
            Class<T> responseType,
            HttpHeaders headers,
            Map<String, ?> pathVariables,
            Map<String, ?> queryParams) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 1) URL 구성 (query 반영)
            UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(url);
            if (queryParams != null && !queryParams.isEmpty()) {
                queryParams.forEach((k, v) -> {
                    if (v != null)
                        ub.queryParam(k, v);
                });
            }

            // 2) PathVariable 확장
            URI uri = (pathVariables == null || pathVariables.isEmpty())
                    ? ub.build(true).toUri()
                    : ub.build(true).expand(pathVariables).toUri();

            // 3) Headers 병합
            HttpHeaders finalHeaders = createDefaultHeaders();
            if (headers != null) {
                // 기존 값 보존 + 덮어쓰기
                headers.forEach((k, v) -> finalHeaders.put(k, v));
            }

            // 4) Entity 구성
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, finalHeaders);

            logger.info("Sending HTTP request - Method: {}, URL: {}", method, uri);

            ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);

            long responseTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            logger.info("HTTP response received - Status: {}, Response Time: {}ms",
                    response.getStatusCode(), responseTime);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestApiException("API returned non-success status: " + response.getStatusCode());
            }
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            long responseTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            logger.error("HTTP request failed - URL: {}, Status: {}, Body: {}, Response Time: {}ms",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), responseTime);
            throw new RestApiException(
                    "Failed to call API: " + url + " - " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);

        } catch (RestClientException e) {
            long responseTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            logger.error("HTTP request failed - URL: {}, Error: {}, Response Time: {}ms",
                    url, e.getMessage(), responseTime);
            throw new RestApiException("Failed to call API: " + url, e);
        }
    }

    /*
     * =========================
     * Helpers
     * =========================
     */

    private HttpHeaders createDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Content-Type이 이미 지정된 경우 덮어쓰지 않음
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static class RestApiException extends RuntimeException {
        public RestApiException(String message) {
            super(message);
        }

        public RestApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
