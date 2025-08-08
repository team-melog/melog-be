package com.melog.melog.adapter.out.persistence.user;

import com.melog.melog.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByNickname(String nickname);
    
    boolean existsByNickname(String nickname);
} 