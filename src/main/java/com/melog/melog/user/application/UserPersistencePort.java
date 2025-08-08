package com.melog.melog.user.application;

import com.melog.melog.user.domain.User;

import java.util.Optional;

public interface UserPersistencePort {
    
    /**
     * 사용자 저장
     */
    User save(User user);
    
    /**
     * ID로 사용자 조회
     */
    Optional<User> findById(Long id);
    
    /**
     * 닉네임으로 사용자 조회
     */
    Optional<User> findByNickname(String nickname);
    
    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(String nickname);
} 