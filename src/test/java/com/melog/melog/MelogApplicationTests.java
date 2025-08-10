package com.melog.melog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.config.import=classpath:clova-properties.yml")
class MelogApplicationTests {

	@Test
	void contextLoads() {
	}

}
