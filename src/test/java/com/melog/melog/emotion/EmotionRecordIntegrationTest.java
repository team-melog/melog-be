package com.melog.melog.emotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.emotion.domain.model.response.EmotionRecordResponse;
import com.melog.melog.user.domain.User;
import com.melog.melog.user.adapter.out.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class EmotionRecordIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 테스트용 사용자 생성
        User testUser = User.builder()
                .nickname("testuser")
                .build();
        userJpaRepository.save(testUser);
    }

    @Test
    void testEmotionRecordWithSTT() throws Exception {
        // 테스트용 음성파일 생성 (실제로는 WAV 또는 MP3 형식)
        String testAudioContent = "테스트 음성 내용입니다.";
        MockMultipartFile audioFile = new MockMultipartFile(
            "audioFile",
            "test_audio.wav",
            "audio/wav",
            testAudioContent.getBytes(StandardCharsets.UTF_8)
        );

        // STT 요청 테스트 - STT 전용 엔드포인트 사용
        mockMvc.perform(multipart("/api/users/testuser/emotions/stt")
                .file(audioFile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").exists());
    }

    @Test
    void testEmotionRecordWithText() throws Exception {
        // 텍스트 요청 테스트 - 텍스트 전용 엔드포인트 사용
        EmotionRecordCreateRequest request = EmotionRecordCreateRequest.builder()
                .text("오늘은 정말 기분이 좋은 하루였습니다.")
                .build();

        mockMvc.perform(post("/api/users/testuser/emotions/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").exists());
    }

    @Test
    void testGetEmotionRecords() throws Exception {
        // 감정 기록 조회 테스트
        mockMvc.perform(get("/api/users/testuser/emotions")
                .param("yearMonth", "2024-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetEmotionRecordById() throws Exception {
        // 먼저 감정 기록을 생성
        EmotionRecordCreateRequest request = EmotionRecordCreateRequest.builder()
                .text("테스트 감정 기록입니다.")
                .build();

        String response = mockMvc.perform(post("/api/users/testuser/emotions/text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        EmotionRecordResponse emotionRecord = objectMapper.readValue(response, EmotionRecordResponse.class);

        // 생성된 감정 기록 조회
        mockMvc.perform(get("/api/users/testuser/emotions/{id}", emotionRecord.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(emotionRecord.getId()))
                .andExpect(jsonPath("$.text").value("테스트 감정 기록입니다."));
    }
}
