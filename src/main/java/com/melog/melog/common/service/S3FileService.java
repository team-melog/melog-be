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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
            
            metadata.addUserMetadata("original-filename", originalFilename);
            metadata.addUserMetadata("upload-date", LocalDateTime.now().toString());
            metadata.addUserMetadata("user-id", userId);

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
     */
    private String generateS3Key(String userId, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        
        return String.format("users/%s/audio/%s/%s/%s", userId, year, month, fileName);
    }

    /**
     * S3 URL을 생성합니다.
     */
    private String generateS3Url(String s3Key) {
        return String.format("%s/%s/%s", endpoint, bucketName, s3Key);
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
        return s3Url.substring(bucketIndex + bucketName.length() + 1);
    }
}
