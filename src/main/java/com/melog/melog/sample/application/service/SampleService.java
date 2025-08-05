package com.melog.melog.sample.application.service;

import org.springframework.stereotype.Service;

import com.melog.melog.sample.application.port.in.SampleUseCase;
import com.melog.melog.sample.application.port.out.SamplePersistencePort;
import com.melog.melog.sample.domain.SampleEntity;
import com.melog.melog.sample.domain.model.request.SampleCreateRequest;
import com.melog.melog.sample.domain.model.response.SampleResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SampleService implements SampleUseCase {

    private final SamplePersistencePort persistencePort;

    @Override
    public SampleResponse create(SampleCreateRequest request) {
        SampleEntity entity = SampleEntity.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        SampleEntity saved = persistencePort.save(entity);

        return new SampleResponse(saved.getId(), saved.getTitle(), saved.getContent());
    }
}