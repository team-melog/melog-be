package com.melog.melog.sample.application.port.in;

import com.melog.melog.sample.domain.model.request.SampleCreateRequest;
import com.melog.melog.sample.domain.model.response.SampleResponse;

public interface SampleUseCase {
    SampleResponse create(SampleCreateRequest request);
}