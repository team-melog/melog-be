package com.melog.melog.clova.domain.model;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "clova")
@Getter
@Setter
public class ClovaConfig {
    private Map<String, ClovaProperties> config;

    public ClovaProperties getProperties(ClovaEndpoint ep) {
        return config.get(ep.name());
    }

}