package com.melog.melog.clova.domain.model.response;

import lombok.Getter;

import java.util.Map;

@Getter
public class ClovaStudioResponse {

    private Status status;
    private Map<String, Object> result;

    @Getter
    public static class Status {
        private String code;
        private String message;
    }
}