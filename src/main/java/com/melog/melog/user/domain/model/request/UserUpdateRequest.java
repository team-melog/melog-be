package com.melog.melog.user.domain.model.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String profileImageUrl;
}
