package com.melog.melog.user.domain.model.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String profileImageUrl;
}
