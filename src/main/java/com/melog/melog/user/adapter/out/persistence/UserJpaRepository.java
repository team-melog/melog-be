package com.melog.melog.user.adapter.out.persistence;

import com.melog.melog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByNickname(String nickname);
    
    boolean existsByNickname(String nickname);
} 