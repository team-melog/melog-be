package com.melog.melog.application.service;

import com.melog.melog.application.port.in.UserUseCase;
import com.melog.melog.application.port.out.UserPersistencePort;
import com.melog.melog.domain.model.request.UserCreateRequest;
import com.melog.melog.domain.model.request.UserUpdateRequest;
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
                .nickname(savedUser.getNickname())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse getUserByNickname(String nickname) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        return UserResponse.builder()
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateUserNickname(String nickname, UserUpdateRequest request) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 새 닉네임 중복 확인
        if (userPersistencePort.existsByNickname(request.getNewNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNewNickname());
        }

        user.updateNickname(request.getNewNickname());
        User updatedUser = userPersistencePort.save(user);

        return UserResponse.builder()
                .nickname(updatedUser.getNickname())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String nickname) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // TODO: 연관된 감정 기록들도 함께 삭제하는 로직 추가 필요
        // userPersistencePort.delete(user);
    }
} 