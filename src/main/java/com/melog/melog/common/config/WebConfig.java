package com.melog.melog.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프로덕션 환경에서 허용할 origin들
        List<String> allowedOrigins = Arrays.asList(
            "https://melog-ai.vercel.app",           // Vercel 프론트엔드
            "https://49.50.134.32",                  // Load Balancer IP
            "https://melog508.duckdns.org",          // DuckDNS 도메인
            "http://localhost:3000",                 // 로컬 개발용
            "http://localhost:8080"                  // 로컬 개발용
        );
        configuration.setAllowedOrigins(allowedOrigins);
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 모든 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Credentials 허용 (구체적인 origin 설정했으므로 true 가능)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return new CorsFilter(source);
    }
}
