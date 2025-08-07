package com.melog.melog.user.application.port.in;

import com.melog.melog.user.domain.model.request.UserCreateRequest;
import com.melog.melog.user.domain.model.request.UserUpdateRequest;
import com.melog.melog.user.domain.model.response.UserResponse;

public interface UserUseCase {
    UserResponse create(UserCreateRequest request);
    UserResponse findById(Long id);
    UserResponse update(Long id, UserUpdateRequest request);
    void delete(Long id);
}
