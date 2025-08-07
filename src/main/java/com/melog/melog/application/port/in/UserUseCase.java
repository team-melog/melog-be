package com.melog.melog.application.port.in;

import com.melog.melog.domain.model.request.UserCreateRequest;
import com.melog.melog.domain.model.response.UserResponse;

public interface UserUseCase {
    
    /**
     * 사용자 생성
     */
    UserResponse createUser(UserCreateRequest request);
    
    /**
     * 사용자 정보 조회
     */
    UserResponse getUserById(Long userId);
} 