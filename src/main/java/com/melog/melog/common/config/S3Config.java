package com.melog.melog.common.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Slf4j
@Configuration
@Profile({"dev", "prod"})
public class S3Config {

    @Value("${NCLOUD_ACCESS_KEY}")
    private String accessKey;

    @Value("${NCLOUD_SECRET_KEY}")
    private String secretKey;

    @Value("${NCLOUD_S3_ENDPOINT}")
    private String endpoint;

    @Value("${NCLOUD_S3_REGION}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        log.info("[S3] endpoint={}, region={}", endpoint, region);
        log.info("[S3] accessKey prefix={}, suffix={}",
            accessKey != null ? accessKey.substring(0, Math.min(4, accessKey.length())) : "null",
            accessKey != null ? accessKey.substring(Math.max(0, accessKey.length()-4)) : "null");
        
        System.out.println("=== S3 Config ===");
        System.out.println("Access Key: " + (accessKey != null ? accessKey.substring(0, 10) + "..." : "null"));
        System.out.println("Secret Key: " + (secretKey != null ? secretKey.substring(0, 10) + "..." : "null"));
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Region: " + region); 
        System.out.println("=================");
        
        // Ncloud Storage 설정 (표준 S3 API 사용)
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
                                endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)))
                .withPathStyleAccessEnabled(true) 
                .build();
    }
}
