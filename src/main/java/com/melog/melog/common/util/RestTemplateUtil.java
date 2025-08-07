package com.melog.melog.common.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestTemplateUtil {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateUtil.class);
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    
    private final RestTemplate restTemplate;

    /**
     * 기본 HTTP 요청 메서드
     */
    public <T> T sendRequest(String url, HttpMethod method, Object requestBody, 
                           Class<T> responseType, HttpHeaders headers) {
        return sendRequestWithRetry(url, method, requestBody, responseType, headers, null, 
                                  DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * Path Variable이 포함된 HTTP 요청 메서드
     */
    public <T> T sendRequestWithPathVariables(String url, HttpMethod method, Object requestBody,
                                            Class<T> responseType, HttpHeaders headers, 
                                            Object[] pathVariables) {
        return sendRequestWithRetry(url, method, requestBody, responseType, headers, pathVariables,
                                  DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * Query Parameter가 포함된 HTTP 요청 메서드
     */
    public <T> T sendRequestWithQueryParams(String url, HttpMethod method, Object requestBody,
                                          Class<T> responseType, HttpHeaders headers,
                                          Map<String, String> queryParams) {
        String urlWithParams = buildUrlWithQueryParams(url, queryParams);
        return sendRequestWithRetry(urlWithParams, method, requestBody, responseType, headers, null,
                                  DEFAULT_MAX_RETRY_ATTEMPTS, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * 재시도 로직이 포함된 내부 요청 메서드
     */
    private <T> T sendRequestWithRetry(String url, HttpMethod method, Object requestBody,
                                     Class<T> responseType, HttpHeaders headers,
                                     Object[] pathVariables, int maxRetryAttempts, long retryDelayMs) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                return executeRequest(url, method, requestBody, responseType, headers, pathVariables);
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

    /**
     * 실제 HTTP 요청 실행
     */
    private <T> T executeRequest(String url, HttpMethod method, Object requestBody,
                               Class<T> responseType, HttpHeaders headers, Object[] pathVariables) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // 기본 헤더 설정
            HttpHeaders finalHeaders = createDefaultHeaders();
            if (headers != null) {
                finalHeaders.putAll(headers);
            }

            HttpEntity<Object> entity = new HttpEntity<>(requestBody, finalHeaders);
            logger.info("Sending HTTP request - Method: {}, URL: {}", method, url);

            ResponseEntity<T> response;
            if (pathVariables != null && pathVariables.length > 0) {
                response = restTemplate.exchange(url, method, entity, responseType, pathVariables);
            } else {
                response = restTemplate.exchange(url, method, entity, responseType);
            }

            long responseTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            logger.info("HTTP response received - Status: {}, Response Time: {}ms", 
                       response.getStatusCode(), responseTime);

            // 응답 상태 코드 검증
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestApiException("API returned non-success status: " + response.getStatusCode());
            }

            return response.getBody();
            
        } catch (RestClientException e) {
            long responseTime = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            logger.error("HTTP request failed - URL: {}, Error: {}, Response Time: {}ms", 
                        url, e.getMessage(), responseTime);
            throw new RestApiException("Failed to call API: " + url, e);
        }
    }

    /**
     * 기본 헤더 생성
     */
    private HttpHeaders createDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Query Parameter를 URL에 추가
     */
    private String buildUrlWithQueryParams(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        
        queryParams.forEach((key, value) -> {
            if (urlBuilder.charAt(urlBuilder.length() - 1) != '?') {
                urlBuilder.append("&");
            }
            urlBuilder.append(key).append("=").append(value);
        });

        return urlBuilder.toString();
    }

    /**
     * REST API 예외 클래스
     */
    public static class RestApiException extends RuntimeException {
        public RestApiException(String message) {
            super(message);
        }
        
        public RestApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
