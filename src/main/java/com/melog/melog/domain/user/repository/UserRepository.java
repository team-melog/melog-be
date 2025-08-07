package com.melog.melog.domain.user.repository;

import com.melog.melog.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    
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
     * 모든 사용자 조회
     */
    List<User> findAll();
    
    /**
     * 사용자 삭제
     */
    void delete(User user);
    
    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(String nickname);
} 