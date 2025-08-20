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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * 바이트 배열을 오디오 파일로 S3에 업로드합니다.
     * TTS 생성 결과나 기타 바이너리 오디오 데이터를 저장할 때 사용됩니다.
     * 
     * 사용 사례:
     * - TTS 서비스에서 생성된 오디오 바이트 데이터 저장
     * - 오디오 변환 결과 저장
     * - 외부 API에서 받은 오디오 데이터 저장
     * 
     * @param audioBytes 업로드할 오디오 바이트 배열
     * @param userId 업로드하는 사용자 ID
     * @param fileExtension 파일 확장자 (예: ".mp3", ".wav")
     * @param originalContentType MIME 타입 (예: "audio/mpeg")
     * @return S3에 저장된 파일의 URL
     * @throws RuntimeException 업로드 실패 시
     * @throws IllegalArgumentException 잘못된 파라미터인 경우
     */
    public String uploadAudioFromByteArray(byte[] audioBytes, String userId, String fileExtension, String originalContentType) {
        // 입력 파라미터 검증
        validateUploadParameters(audioBytes, userId, fileExtension);
        
        try {
            // 파일명 생성 (중복 방지를 위한 타임스탬프 + UUID 조합)
            String fileName = generateFileName(userId, fileExtension);
            
            // S3 키 생성 (폴더 구조: users/{userId}/audio/{year}/{month}/{fileName})
            String s3Key = generateS3Key(userId, fileName);
            
            // 메타데이터 설정
            ObjectMetadata metadata = createAudioMetadata(audioBytes.length, originalContentType, fileExtension, userId);
            
            // 바이트 배열을 InputStream으로 변환하여 업로드
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(audioBytes)) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucketName, s3Key, inputStream, metadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead);
                
                amazonS3.putObject(putObjectRequest);
            }
            
            // S3 URL 생성
            String s3Url = generateS3Url(s3Key);
            
            log.info("바이트 배열 오디오 업로드 성공: bucket={}, key={}, size={}bytes, contentType={}", 
                    bucketName, s3Key, audioBytes.length, originalContentType);
            
            return s3Url;
            
        } catch (Exception e) {
            log.error("바이트 배열 오디오 업로드 실패: userId={}, extension={}, size={}bytes, error={}", 
                    userId, fileExtension, audioBytes.length, e.getMessage(), e);
            throw new RuntimeException("오디오 파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 업로드 파라미터 유효성 검증
     * 
     * @param audioBytes 오디오 바이트 배열
     * @param userId 사용자 ID
     * @param fileExtension 파일 확장자
     * @throws IllegalArgumentException 잘못된 파라미터인 경우
     */
    private void validateUploadParameters(byte[] audioBytes, String userId, String fileExtension) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("오디오 데이터가 비어있습니다");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID가 필요합니다");
        }
        
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다");
        }
        
        // 파일 크기 제한 (최대 50MB)
        if (audioBytes.length > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기가 너무 큽니다. 최대 50MB까지 지원됩니다");
        }
        
        // 최소 파일 크기 제한 (1KB)
        if (audioBytes.length < 1024) {
            throw new IllegalArgumentException("파일 크기가 너무 작습니다. 최소 1KB 이상이어야 합니다");
        }
    }

    /**
     * 오디오 파일용 메타데이터 생성
     * 
     * @param contentLength 파일 크기
     * @param originalContentType 원본 MIME 타입
     * @param fileExtension 파일 확장자
     * @param userId 사용자 ID
     * @return ObjectMetadata 인스턴스
     */
    private ObjectMetadata createAudioMetadata(long contentLength, String originalContentType, String fileExtension, String userId) {
        ObjectMetadata metadata = new ObjectMetadata();
        
        // Content-Type 설정 (원본이 없으면 확장자 기반으로 추론)
        String contentType = determineContentType(originalContentType, fileExtension);
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        
        // 오디오 스트리밍 최적화를 위한 캐시 설정
        // immutable: 파일이 변경되지 않음을 명시하여 브라우저 캐싱 최적화
        // metadata.setCacheControl("public, max-age=31536000, immutable");
        
        // 사용자 정의 메타데이터 추가 (관리 및 디버깅 목적)
        metadata.addUserMetadata("upload-type", "byte-array");
        metadata.addUserMetadata("upload-date", LocalDateTime.now().toString());
        // metadata.addUserMetadata("user-id", userId);
        metadata.addUserMetadata("file-extension", fileExtension);
        
        return metadata;
    }

    /**
     * Content-Type 결정 로직
     * 원본 Content-Type이 있으면 사용하고, 없으면 파일 확장자 기반으로 추론합니다.
     * 
     * @param originalContentType 원본 MIME 타입
     * @param fileExtension 파일 확장자
     * @return 결정된 Content-Type
     */
    private String determineContentType(String originalContentType, String fileExtension) {
        // 원본 Content-Type이 유효하면 그대로 사용
        if (originalContentType != null && !originalContentType.trim().isEmpty()) {
            return originalContentType;
        }
        
        // 파일 확장자 기반으로 Content-Type 추론
        String extension = fileExtension.replace(".", "").toLowerCase();
        return switch (extension) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            default -> "application/octet-stream"; // 알 수 없는 형식은 바이너리로 처리
        };
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
        
        try {
            // 한글 닉네임을 URL 인코딩하여 S3 키에 안전하게 포함
            String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8.toString());
            return String.format("users/%s/audio/%s/%s/%s", encodedUserId, year, month, fileName);
        } catch (Exception e) {
            log.warn("URL 인코딩 실패, 원본 userId 사용: {}", userId);
            // 인코딩 실패 시 원본 userId 사용
            return String.format("users/%s/audio/%s/%s/%s", userId, year, month, fileName);
        }
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
        String s3Key = s3Url.substring(bucketIndex + bucketName.length() + 1);
        
        try {
            // URL 인코딩된 키를 디코딩하여 반환
            return URLDecoder.decode(s3Key, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            log.warn("URL 디코딩 실패, 원본 키 사용: {}", s3Key);
            // 디코딩 실패 시 원본 키 사용
            return s3Key;
        }
    }
}
