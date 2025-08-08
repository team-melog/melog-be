package com.melog.melog.user.application;

import com.melog.melog.common.model.request.UserCreateRequest;
import com.melog.melog.common.model.request.UserUpdateRequest;
import com.melog.melog.common.model.response.UserResponse;

public interface UserUseCase {
    
    /**
     * 사용자 생성
     */
    UserResponse createUser(UserCreateRequest request);
    
    /**
     * 사용자 정보 조회 (닉네임 기반)
     */
    UserResponse getUserByNickname(String nickname);
    
    /**
     * 사용자 닉네임 수정
     */
    UserResponse updateUserNickname(String nickname, UserUpdateRequest request);
    
    /**
     * 사용자 삭제
     */
    void deleteUser(String nickname);
} 