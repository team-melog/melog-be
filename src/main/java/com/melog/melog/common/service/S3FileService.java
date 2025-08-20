package com.melog.melog.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final AmazonS3 amazonS3;

    @Value("${NCLOUD_S3_BUCKET}")
    private String bucketName;

    @Value("${NCLOUD_S3_ENDPOINT}")
    private String endpoint;

    /**
     * 음성 파일을 S3에 업로드합니다.
     */
    public String uploadAudioFile(MultipartFile file, String userId) {
        try {
            // 파일명 생성 (중복 방지)
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateFileName(userId, fileExtension);
            
            // S3 키 생성 (폴더 구조: users/{userId}/audio/{year}/{month}/{fileName})
            String s3Key = generateS3Key(userId, fileName);
            
            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            
            // contentType null 대비 및 확장자 기반 설정
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                String ext = fileExtension.replace(".", "").toLowerCase();
                contentType = ("wav".equals(ext) || "x-wav".equals(ext)) ? "audio/wav"
                             : ("mp3".equals(ext)) ? "audio/mpeg"
                             : ("m4a".equals(ext)) ? "audio/mp4"
                             : "application/octet-stream";
            }
            metadata.setContentType(contentType);
            metadata.setContentLength(file.getSize());
            
            // Cache-Control 설정 (오디오 스트리밍 최적화)
            metadata.setCacheControl("public, max-age=31536000, immutable");
            
            // user metadata를 ASCII로 강제 (S3 서명 문제 방지)
            putAsciiUserMeta(metadata, "original-filename", originalFilename);
            putAsciiUserMeta(metadata, "upload-date", LocalDateTime.now().toString());
            putAsciiUserMeta(metadata, "user-id", userId);

            // 파일 업로드 (퍼블릭 읽기 허용)
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, s3Key, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead); // ★ 퍼블릭 읽기

            amazonS3.putObject(putObjectRequest);

            // S3 URL 생성
            String s3Url = generateS3Url(s3Key);
            
            log.info("파일 업로드 성공: bucket={}, key={}, size={}bytes", 
                    bucketName, s3Key, file.getSize());
            
            return s3Url;
            
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     */
    public void deleteFile(String s3Url) {
        try {
            String s3Key = extractS3KeyFromUrl(s3Url);
            
            amazonS3.deleteObject(bucketName, s3Key);
            
            log.info("파일 삭제 성공: bucket={}, key={}", bucketName, s3Key);
            
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일이 S3에 존재하는지 확인합니다.
     */
    public boolean fileExists(String s3Url) {
        try {
            String s3Key = extractS3KeyFromUrl(s3Url);
            
            return amazonS3.doesObjectExist(bucketName, s3Key);
            
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 파일 확장자를 추출합니다.
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 고유한 파일명을 생성합니다.
     */
    private String generateFileName(String userId, String fileExtension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s%s", userId, timestamp, uuid, fileExtension);
    }

    /**
     * S3 키를 생성합니다.
     * S3 업로드용이므로 원본 그대로 사용 (URL 인코딩하지 않음)
     */
    private String generateS3Key(String userId, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        
        // S3 키는 원본 그대로 사용 (SDK가 알아서 처리)
        return String.format("users/%s/audio/%s/%s/%s", userId, year, month, fileName);
    }

    /**
     * S3 URL을 생성합니다.
     * 공개 URL용이므로 경로 세그먼트별로 URL 인코딩 (슬래시는 보존)
     */
    private String generateS3Url(String s3Key) {
        try {
            // 경로 세그먼트별로 인코딩 (슬래시는 보존)
            String encodedKey = encodePathForUrl(s3Key);
            return String.format("%s/%s/%s", endpoint, bucketName, encodedKey);
        } catch (Exception e) {
            log.warn("S3 URL 생성 중 에러 발생, 원본 키 사용: {}", e.getMessage());
            return String.format("%s/%s/%s", endpoint, bucketName, s3Key);
        }
    }
    
    /**
     * URL 표시용으로 경로 세그먼트별로 인코딩합니다.
     * 슬래시는 보존하고, 각 세그먼트만 인코딩합니다.
     */
    private String encodePathForUrl(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        
        return Arrays.stream(key.split("/"))
            .map(segment -> {
                try {
                    // 각 세그먼트를 URL 인코딩하고, +를 %20으로 변환
                    return URLEncoder.encode(segment, StandardCharsets.UTF_8.toString())
                                   .replace("+", "%20");
                } catch (Exception e) {
                    log.warn("세그먼트 인코딩 실패: {}", segment);
                    return segment;
                }
            })
            .collect(Collectors.joining("/")); // 슬래시는 보존
    }
    
    /**
     * user metadata를 ASCII로 강제하여 S3 서명 문제를 방지합니다.
     * 비-ASCII 값은 Base64로 인코딩하여 저장합니다.
     */
    private void putAsciiUserMeta(ObjectMetadata metadata, String key, String value) {
        if (value == null) return;
        
        boolean isAscii = StandardCharsets.US_ASCII.newEncoder().canEncode(value);
        if (isAscii) {
            // ASCII인 경우 그대로 저장
            metadata.addUserMetadata(key, value);
        } else {
            // 비-ASCII인 경우 Base64로 인코딩하여 저장
            String encodedValue = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            metadata.addUserMetadata(key + "-utf8-b64", encodedValue);
            log.info("비-ASCII user metadata를 Base64로 인코딩: {}={} -> {}-utf8-b64={}", 
                    key, value, key, encodedValue);
        }
    }

    /**
     * S3 URL에서 키를 추출합니다.
     * ⚠️ 주의: 엔드포인트/버킷이 바뀌면 반드시 재확인 필요
     */
    private String extractS3KeyFromUrl(String s3Url) {
        if (s3Url == null || !s3Url.contains(bucketName)) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + s3Url);
        }
        
        int bucketIndex = s3Url.indexOf(bucketName);
        String s3Key = s3Url.substring(bucketIndex + bucketName.length() + 1);
        
        // URL에서 추출한 키는 이미 인코딩되어 있으므로 디코딩하여 원본 키 반환
        try {
            return URLDecoder.decode(s3Key, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.warn("URL 디코딩 실패, 원본 키 사용: {}", s3Key);
            return s3Key;
        }
    }
}
