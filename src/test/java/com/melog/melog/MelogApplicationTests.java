package com.melog.melog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "CLOVA_SPEECH_CLIENT_ID=test-client-id",
    "CLOVA_SPEECH_CLIENT_SECRET=test-client-secret",
    "CLOVA_STUDIO_API_KEY=test-api-key"
})
class MelogApplicationTests {

	@Test
	void contextLoads() {
	}

}
