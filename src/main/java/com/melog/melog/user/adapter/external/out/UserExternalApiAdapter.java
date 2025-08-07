package com.melog.melog.user.adapter.external.out;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.melog.melog.common.util.RestTemplateUtil;
import com.melog.melog.user.application.port.out.UserExternalApiPort;
import com.melog.melog.user.domain.model.response.ExternalUserInfoResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserExternalApiAdapter implements UserExternalApiPort {

    private final RestTemplateUtil restTemplateUtil;

    @Override
    public ExternalUserInfoResponse getUserInfoFromExternalApi(String userId) {
        // Path Variable 사용 예시
        Object[] pathVariables = {userId};
        
        return restTemplateUtil.sendRequestWithPathVariables(
                "https://api.external-service.com/users/{userId}",
                org.springframework.http.HttpMethod.GET,
                null,
                ExternalUserInfoResponse.class,
                createExternalApiHeaders(),
                pathVariables
        );
    }

    @Override
    public ExternalUserInfoResponse searchUsers(Map<String, String> searchParams) {
        // Query Parameter 사용 예시
        return restTemplateUtil.sendRequestWithQueryParams(
                "https://api.external-service.com/users/search",
                org.springframework.http.HttpMethod.GET,
                null,
                ExternalUserInfoResponse.class,
                createExternalApiHeaders(),
                searchParams
        );
    }

    @Override
    public ExternalUserInfoResponse createUser(Object userData) {
        // 기본 POST 요청 예시
        return restTemplateUtil.sendRequest(
                "https://api.external-service.com/users",
                org.springframework.http.HttpMethod.POST,
                userData,
                ExternalUserInfoResponse.class,
                createExternalApiHeaders()
        );
    }

    @Override
    public ExternalUserInfoResponse updateUserWithRetry(String userId, Object userData, int maxRetries) {
        // 커스텀 재시도 설정 예시
        Object[] pathVariables = {userId};
        
        return restTemplateUtil.sendRequestWithCustomRetry(
                "https://api.external-service.com/users/{userId}",
                org.springframework.http.HttpMethod.PUT,
                userData,
                ExternalUserInfoResponse.class,
                createExternalApiHeaders(),
                pathVariables,
                maxRetries,
                2000 // 2초 대기
        );
    }

    /**
     * 외부 API 전용 헤더 생성
     */
    private org.springframework.http.HttpHeaders createExternalApiHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer external-api-token");
        headers.set("X-API-Version", "v2");
        headers.set("X-Client-ID", "melog-app");
        return headers;
    }
}
