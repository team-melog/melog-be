package com.melog.melog.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 모든 origin 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // preflight 요청 캐시 시간 (1시간)
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 모든 origin 허용
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 모든 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Credentials 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return new CorsFilter(source);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 모든 origin 허용
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 모든 헤더 노출
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Credentials 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
