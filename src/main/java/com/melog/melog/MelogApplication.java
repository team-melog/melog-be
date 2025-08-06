package com.melog.melog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.melog.melog.clova.domain.model.ClovaConfig;

@SpringBootApplication
@EnableConfigurationProperties(ClovaConfig.class)
public class MelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelogApplication.class, args);
	}

}
