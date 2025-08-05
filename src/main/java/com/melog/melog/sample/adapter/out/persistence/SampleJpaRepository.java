package com.melog.melog.sample.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.melog.melog.sample.domain.SampleEntity;

public interface SampleJpaRepository extends JpaRepository<SampleEntity, Long> {
}