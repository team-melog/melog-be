package com.melog.melog.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 설정
 * 
 * Swagger UI 및 OpenAPI 문서 생성을 위한 설정 클래스입니다.
 * API 문서의 메타정보, 서버 정보, 보안 설정 등을 정의합니다.
 * 
 * 접근 URL:
 * - Swagger UI: /swagger-ui/index.html
 * - OpenAPI JSON: /v3/api-docs
 * 
 * @author Melog Team
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 설정 Bean
     * 
     * API 문서의 기본 정보와 서버 설정을 정의합니다.
     * 개발 및 운영 환경에 따라 서버 URL을 동적으로 설정할 수 있습니다.
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI melogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Melog API")
                        .description("감정 기록 및 음성 서비스 API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Melog Team")
                                .email("team@melog.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("개발 서버"),
                        new Server()
                                .url("https://api.melog.com")
                                .description("운영 서버")
                ));
    }
}