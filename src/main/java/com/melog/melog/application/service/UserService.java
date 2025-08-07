package com.melog.melog.application.service;

import com.melog.melog.application.port.in.UserUseCase;
import com.melog.melog.application.port.out.UserPersistencePort;
import com.melog.melog.domain.model.request.UserCreateRequest;
import com.melog.melog.domain.model.response.UserResponse;
import com.melog.melog.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

    private final UserPersistencePort userPersistencePort;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 닉네임 중복 확인
        if (userPersistencePort.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNickname());
        }

        // 사용자 생성
        User user = User.builder()
                .nickname(request.getNickname())
                .build();

        User savedUser = userPersistencePort.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .nickname(savedUser.getNickname())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .build();
    }
} 