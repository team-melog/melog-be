package com.melog.melog.user.application.service;

import com.melog.melog.user.domain.model.request.UserCreateRequest;
import com.melog.melog.user.domain.model.request.UserUpdateRequest;
import com.melog.melog.user.domain.model.response.UserResponse;
import com.melog.melog.user.application.port.in.UserUseCase;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.emotion.application.port.out.EmotionRecordPersistencePort;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 닉네임 중복 확인
        if (userPersistencePort.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNickname());
        }

        // 사용자 생성
        User user = User.builder()
                .nickname(request.getNickname())
                .build();

        User savedUser = userPersistencePort.save(user);

        return UserResponse.createDefault(
                savedUser.getId(),
                savedUser.getNickname(),
                savedUser.getCreatedAt()
        );
    }

    @Override
    public UserResponse getUserByNickname(String nickname) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 실제 통계 데이터 계산
        Integer emotionCount = (int) emotionRecordPersistencePort.countByUser(user);
        Integer audioCount = (int) emotionRecordPersistencePort.countByUserAndAudioFilePathIsNotNull(user);
        
        // 대표 감정 계산 (가장 최근 감정 기록에서 가장 높은 감정)
        UserResponse.RepresentativeEmotionResponse representativeEmotion = calculateRepresentativeEmotion(user);

        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .emotionCount(emotionCount)
                .audioCount(audioCount)
                .representativeEmotion(representativeEmotion)
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateUserNickname(String nickname, UserUpdateRequest request) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // 새 닉네임 중복 확인
        if (userPersistencePort.existsByNickname(request.getNewNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다: " + request.getNewNickname());
        }

        user.updateNickname(request.getNewNickname());
        User updatedUser = userPersistencePort.save(user);

        return UserResponse.createDefault(
                updatedUser.getId(),
                updatedUser.getNickname(),
                updatedUser.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteUser(String nickname) {
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + nickname));

        // TODO: 연관된 감정 기록들도 함께 삭제하는 로직 추가 필요
        // userPersistencePort.delete(user);
    }
    
    /**
     * 사용자의 대표 감정을 계산합니다.
     */
    private UserResponse.RepresentativeEmotionResponse calculateRepresentativeEmotion(User user) {
        try {
            // 가장 최근 감정 기록 조회
            var recentRecords = emotionRecordPersistencePort.findByUser(user);
            if (recentRecords.isEmpty()) {
                return UserResponse.RepresentativeEmotionResponse.getDefault();
            }
            
            // 가장 최근 기록에서 가장 높은 감정 점수를 가진 감정 찾기
            var mostRecentRecord = recentRecords.stream()
                    .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .orElse(null);
            
            if (mostRecentRecord == null || mostRecentRecord.getEmotionScores().isEmpty()) {
                return UserResponse.RepresentativeEmotionResponse.getDefault();
            }
            
            var primaryEmotion = mostRecentRecord.getPrimaryEmotion();
            if (primaryEmotion == null) {
                return UserResponse.RepresentativeEmotionResponse.getDefault();
            }
            
            return UserResponse.RepresentativeEmotionResponse.builder()
                    .type(primaryEmotion.getEmotionType().getDescription())
                    .step(primaryEmotion.getStep())
                    .build();
                    
        } catch (Exception e) {
            // 에러 발생 시 기본값 반환
            return UserResponse.RepresentativeEmotionResponse.getDefault();
        }
    }
} 