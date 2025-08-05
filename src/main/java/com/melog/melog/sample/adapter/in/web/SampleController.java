package com.melog.melog.sample.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.melog.melog.sample.application.port.in.SampleUseCase;
import com.melog.melog.sample.domain.model.request.SampleCreateRequest;
import com.melog.melog.sample.domain.model.response.SampleResponse;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/sample")
@RequiredArgsConstructor
public class SampleController {

    private final SampleUseCase sampleUseCase;

    @PostMapping
    public SampleResponse create(@RequestBody SampleCreateRequest request) {
        return sampleUseCase.create(request);
    }
}