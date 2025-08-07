package com.melog.melog.user.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.user.application.port.in.UserUseCase;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.UserEntity;
import com.melog.melog.user.domain.model.request.UserCreateRequest;
import com.melog.melog.user.domain.model.request.UserUpdateRequest;
import com.melog.melog.user.domain.model.response.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserPersistencePort persistencePort;

    @Override
    public UserResponse create(UserCreateRequest request) {
        UserEntity entity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        UserEntity saved = persistencePort.save(entity);

        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getNickname(),
                saved.getProfileImageUrl()
        );
    }

    @Override
    public UserResponse findById(Long id) {
        UserEntity user = persistencePort.findById(id);
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }

    @Override
    public UserResponse update(Long id, UserUpdateRequest request) {
        UserEntity user = persistencePort.findById(id);
        
        UserEntity updatedUser = UserEntity.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        UserEntity saved = persistencePort.save(updatedUser);

        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getNickname(),
                saved.getProfileImageUrl()
        );
    }

    @Override
    public void delete(Long id) {
        persistencePort.deleteById(id);
    }
}
