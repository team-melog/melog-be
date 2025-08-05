package com.melog.melog.sample.domain.model.request;

import lombok.Data;

@Data
public class SampleCreateRequest {
    private String title;
    private String content;
}