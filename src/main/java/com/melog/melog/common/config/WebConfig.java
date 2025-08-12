package com.melog.melog.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 개발 환경에서는 모든 origin 허용, 프로덕션에서는 구체적인 origin 지정
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 모든 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Credentials 허용 (주의: allowCredentials가 true일 때는 구체적인 origin 지정 권장)
        configuration.setAllowCredentials(false); // true로 설정하면 구체적인 origin 필요
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return new CorsFilter(source);
    }
}
