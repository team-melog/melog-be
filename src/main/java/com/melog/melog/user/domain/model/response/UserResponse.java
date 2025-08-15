package com.melog.melog.user.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String nickname;
    private LocalDateTime createdAt;
    private Integer emotionCount; // 감정 기록 수
    private Integer audioCount; // 음성 파일 수
    private RepresentativeEmotionResponse representativeEmotion; // 대표 감정
    
    @Getter
    @Builder
    public static class RepresentativeEmotionResponse {
        private String type; // 감정 타입 (한글)
        private Integer step; // 감정 단계
        
        // 기본값 생성
        public static RepresentativeEmotionResponse getDefault() {
            return RepresentativeEmotionResponse.builder()
                    .type("평온")
                    .step(1)
                    .build();
        }
    }
    
    // 기본값을 가진 UserResponse 생성
    public static UserResponse createDefault(Long id, String nickname, LocalDateTime createdAt) {
        return UserResponse.builder()
                .id(id)
                .nickname(nickname)
                .createdAt(createdAt)
                .emotionCount(0)
                .audioCount(0)
                .representativeEmotion(RepresentativeEmotionResponse.getDefault())
                .build();
    }
    
    // null 체크 및 기본값 반환 메서드들
    public Integer getEmotionCount() {
        return emotionCount != null ? emotionCount : 0;
    }
    
    public Integer getAudioCount() {
        return audioCount != null ? audioCount : 0;
    }
    
    public RepresentativeEmotionResponse getRepresentativeEmotion() {
        return representativeEmotion != null ? representativeEmotion : RepresentativeEmotionResponse.getDefault();
    }
} 