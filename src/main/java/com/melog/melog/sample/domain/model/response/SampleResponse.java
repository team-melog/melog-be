package com.melog.melog.sample.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SampleResponse {
    private Long id;
    private String title;
    private String content;
}
