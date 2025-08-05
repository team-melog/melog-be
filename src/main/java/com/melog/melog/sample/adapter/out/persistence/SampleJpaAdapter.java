package com.melog.melog.sample.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.melog.melog.sample.application.port.out.SamplePersistencePort;
import com.melog.melog.sample.domain.SampleEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SampleJpaAdapter implements SamplePersistencePort {
    private final SampleJpaRepository repository;

    @Override
    public SampleEntity save(SampleEntity entity) {
        return repository.save(entity);
    }
}