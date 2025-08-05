package com.melog.melog.sample.application.port.out;

import com.melog.melog.sample.domain.SampleEntity;

public interface SamplePersistencePort {
    SampleEntity save(SampleEntity entity);
}