package com.melog.melog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan(basePackages = "com.melog.melog")
public class MelogApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelogApplication.class, args);
	} 

}
