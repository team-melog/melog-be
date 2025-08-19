package com.melog.melog.common.service;

import com.melog.melog.emotion.application.service.EmotionRecordService;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.emotion.domain.model.request.EmotionRecordSelectRequest;
import com.melog.melog.emotion.domain.model.response.EmotionRecordResponse;
import com.melog.melog.emotion.domain.model.response.EmotionScoreResponse;
import com.melog.melog.user.domain.User;
import com.melog.melog.user.domain.model.request.UserCreateRequest;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.application.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DummyDataService {

    private final EmotionRecordService emotionRecordService; // Interface가 아닌 Service 직접 주입
    private final UserService userService;
    private final UserPersistencePort userPersistencePort;
    private final ObjectMapper objectMapper;

    private static final int DUMMY_COUNT = 30;

    /**
     * 더미 유저 생성 (중복 방지)
     */
    public void createDummyUser() {
        String nickname = "dummyuser";
        
        try {
            userService.createUser(new UserCreateRequest(nickname));
            log.info("더미 유저 생성 완료: {}", nickname);
        } catch (Exception e) {
            log.error("더미유저 생성 실패", e);
        }
    }

    /**
     * emotion_requests_dummy.json을 이용한 더미 감정 기록 생성 (중복 방지)
     */
    public void createDummyEmotionRecords() {
        String nickname = "dummyuser";
        
        // 사용자 조회
        User user = userPersistencePort.findByNickname(nickname).orElse(null);
        if (user == null) {
            log.error("더미 유저를 찾을 수 없습니다: {}", nickname);
            return;
        }

        try {
            // JSON 파일 읽기
            ClassPathResource resource = new ClassPathResource("emotion_requests_dummy.json");
            List<EmotionRequestDto> requests = objectMapper.readValue(
                resource.getInputStream(), 
                new TypeReference<List<EmotionRequestDto>>() {}
            );

            int successCount = 0;
            int skipCount = 0;

            // 최근 30일간 분산하여 생성, 최대 50개.
            for (int i = 0; i < Math.min(requests.size(), DUMMY_COUNT); i++) {
                EmotionRequestDto requestDto = requests.get(i);
                LocalDate targetDate = LocalDate.now().minusDays(i + 1);
                
                try {

                    // EmotionRecordCreateRequest 생성
                    EmotionRecordCreateRequest.UserSelectedEmotion userSelectedEmotion = 
                        EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                            .type(requestDto.getUserSelectedEmotion().getType())
                            .percentage(requestDto.getUserSelectedEmotion().getPercentage())
                            .build();

                    

                    EmotionRecordCreateRequest request = EmotionRecordCreateRequest.builder()
                            .text(requestDto.getText())
                            .userSelectedEmotion(userSelectedEmotion)
                            .build();


                    // Service의 overload 메소드 직접 호출 (Interface 메소드가 아님)
                    EmotionRecordResponse response = emotionRecordService.createEmotionRecordWithDate(
                        nickname, request, targetDate
                    );


                    log.info("감정 기록 생성 완료: date={}, id={}", targetDate, response.getId());

                    // AI 분석 결과를 채택하여 업데이트
                    adoptAIRecommendation(nickname, response);
                    
                    successCount++;


                } catch (Exception e) {
                    log.error("감정 기록 생성 실패: date={}, error={}", targetDate, e.getMessage());
                }
            }

            log.info("더미 감정 기록 생성 완료 - 성공: {}개, 건너뜀: {}개", successCount, skipCount);

        } catch (IOException e) {
            log.error("emotion_requests_dummy.json 파일 읽기 실패", e);
        }
    }

    /**
     * AI 분석 결과를 채택하여 감정 선택 업데이트
     */
    private void adoptAIRecommendation(String nickname, EmotionRecordResponse response) {
        try {
            
            List<EmotionRecordSelectRequest.EmotionSelection> emotions = new ArrayList<>();

            for(EmotionScoreResponse emotion : response.getEmotions()){
                emotions.add(new EmotionRecordSelectRequest.EmotionSelection(
                                emotion.getEmotionType(),
                                emotion.getPercentage()
                            ));
            }

            EmotionRecordSelectRequest selectRequest = new EmotionRecordSelectRequest(emotions);

                    // AI 추천값으로 업데이트
            emotionRecordService.updateEmotionSelection(nickname, response.getId(), selectRequest);
            log.debug("AI 추천값 채택 완료: recordId={}, emotions={}", response.getId(), emotions);
        } catch (Exception e) {
            log.error("AI 추천값 채택 실패: recordId={}, error={}", response.getId(), e.getMessage());
        }
    }

    /**
     * 전체 더미 데이터 초기화 (중복 방지)
     */
    public void initializeAllDummyData() {
        log.info("더미 데이터 초기화 시작");
        
        // 1. 더미 유저 생성 (중복 방지)
        createDummyUser();
        
        // 2. 더미 감정 기록 생성 (중복 방지)
        createDummyEmotionRecords();
        
        log.info("더미 데이터 초기화 완료");
    }

    /**
     * emotion_requests_dummy.json 구조와 매핑되는 DTO
     */
    public static class EmotionRequestDto {
        private String text;
        private UserSelectedEmotionDto userSelectedEmotion;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public UserSelectedEmotionDto getUserSelectedEmotion() { return userSelectedEmotion; }
        public void setUserSelectedEmotion(UserSelectedEmotionDto userSelectedEmotion) { this.userSelectedEmotion = userSelectedEmotion; }

        public static class UserSelectedEmotionDto {
            private String type;
            private Integer percentage;

            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public Integer getPercentage() { return percentage; }
            public void setPercentage(Integer percentage) { this.percentage = percentage; }
        }
    }
}