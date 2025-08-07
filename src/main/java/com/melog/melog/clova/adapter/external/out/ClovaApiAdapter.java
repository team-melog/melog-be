package com.melog.melog.clova.adapter.external.out;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.melog.melog.clova.domain.model.ClovaConfig;
import com.melog.melog.clova.domain.model.ClovaEndpoint;
import com.melog.melog.clova.domain.model.ClovaProperties;
import com.melog.melog.common.util.RestTemplateUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClovaApiAdapter {

    private final RestTemplateUtil restTemplateUtil;
    private final ClovaConfig clovaConfig;

    /**
     * Clova API에 공통적으로 사용되는 HTTP 요청 메서드
     * @param endpoint Clova 엔드포인트
     * @param requestBody 요청 본문
     * @param responseType 응답 타입
     * @return API 응답
     */
    public <T> T sendRequest(ClovaEndpoint endpoint, Object requestBody, Class<T> responseType) {
        return sendRequestInternal(endpoint, null, requestBody, responseType);
    }

    /**
     * Path Variable이 포함된 요청을 위한 메서드
     * @param endpoint Clova 엔드포인트
     * @param pathVariables 경로 변수들
     * @param requestBody 요청 본문
     * @param responseType 응답 타입
     * @return API 응답
     */
    public <T> T sendRequestWithPathVariables(ClovaEndpoint endpoint, 
                                            Object[] pathVariables, 
                                            Object requestBody, 
                                            Class<T> responseType) {
        return sendRequestInternal(endpoint, pathVariables, requestBody, responseType);
    }

    /**
     * 내부 요청 처리 메서드
     */
    private <T> T sendRequestInternal(ClovaEndpoint endpoint, 
                                    Object[] pathVariables, 
                                    Object requestBody, 
                                    Class<T> responseType) {
        ClovaProperties props = clovaConfig.getProperties(endpoint);
        if (props == null) {
            throw new IllegalArgumentException("No Clova config found for endpoint: " + endpoint);
        }

        String url = props.getUrl() + endpoint.getUrl();
        HttpHeaders headers = createHeaders(endpoint, props);

        try {
            if (pathVariables != null && pathVariables.length > 0) {
                return restTemplateUtil.sendRequestWithPathVariables(
                    url, endpoint.getMethod(), requestBody, responseType, headers, pathVariables);
            } else {
                return restTemplateUtil.sendRequest(
                    url, endpoint.getMethod(), requestBody, responseType, headers);
            }
        } catch (RestTemplateUtil.RestApiException e) {
            throw new ClovaApiException("Failed to call Clova API: " + endpoint, e);
        }
    }

    /**
     * 헤더 생성 및 인증 설정
     */
    private HttpHeaders createHeaders(ClovaEndpoint endpoint, ClovaProperties props) {
        HttpHeaders headers = new HttpHeaders();
        
        // 엔드포인트별 인증 전략 적용
        endpoint.getAuthStrategy().accept(headers, props);
        
        return headers;
    }

    /**
     * Clova API 예외 클래스
     */
    public static class ClovaApiException extends RuntimeException {
        public ClovaApiException(String message) {
            super(message);
        }
        
        public ClovaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
