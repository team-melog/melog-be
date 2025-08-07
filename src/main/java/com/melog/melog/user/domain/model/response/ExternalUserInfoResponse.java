package com.melog.melog.user.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUserInfoResponse {
    private String id;
    private String name;
    private String email;
    private String status;
    private String externalId;
}
