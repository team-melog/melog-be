package com.melog.melog.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String nickname;
    private LocalDateTime createdAt;
} 