package com.melog.melog.user.application.port.out;

import java.util.Map;

import com.melog.melog.user.domain.model.response.ExternalUserInfoResponse;

public interface UserExternalApiPort {
    ExternalUserInfoResponse getUserInfoFromExternalApi(String userId);
    ExternalUserInfoResponse searchUsers(Map<String, String> searchParams);
    ExternalUserInfoResponse createUser(Object userData);
    ExternalUserInfoResponse updateUserWithRetry(String userId, Object userData, int maxRetries);
}
