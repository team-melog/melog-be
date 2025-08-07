package com.melog.melog.user.application.port.out;

import com.melog.melog.user.domain.UserEntity;

public interface UserPersistencePort {
    UserEntity save(UserEntity user);
    UserEntity findById(Long id);
    void deleteById(Long id);
}
