package com.melog.melog.clova;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.melog.melog.clova.application.port.out.SpeechToTextPort;
import com.melog.melog.clova.application.service.SpeechToTextService;
import com.melog.melog.clova.domain.model.request.SttRequest;
import com.melog.melog.clova.domain.model.response.SttResponse;

@ExtendWith(MockitoExtension.class)
class SpeechToTextServiceTest {

    @Mock
    private SpeechToTextPort speechToTextPort;

    @InjectMocks
    private SpeechToTextService speechToTextService;

    private MockMultipartFile mockAudioFile;

    @BeforeEach
    void setUp() {
        // 테스트용 오디오 파일 생성 (1KB 크기)
        byte[] audioData = new byte[1024];
        mockAudioFile = new MockMultipartFile(
            "audio", 
            "test.mp3", 
            "audio/mpeg", 
            audioData
        );
    }

    @Test
    void recognize_ValidAudioFile_ReturnsSttResponse() {
        // Given
        SttResponse expectedResponse = SttResponse.builder()
            .text("안녕하세요")
            .language("Kor")
            .quota(5)
            .build();

        when(speechToTextPort.sendSpeechToTextRequest(any(SttRequest.class)))
            .thenReturn(expectedResponse);

        // When
        SttResponse result = speechToTextService.recognize(mockAudioFile, "ko");

        // Then
        assertNotNull(result);
        assertEquals("안녕하세요", result.getText());
        assertEquals("Kor", result.getLanguage());
    }

    @Test
    void recognizeToText_ValidAudioFile_ReturnsText() {
        // Given
        SttResponse mockResponse = SttResponse.builder()
            .text("테스트 음성입니다")
            .language("Kor")
            .build();

        when(speechToTextPort.sendSpeechToTextRequest(any(SttRequest.class)))
            .thenReturn(mockResponse);

        // When
        String result = speechToTextService.recognizeToText(mockAudioFile, "ko");

        // Then
        assertEquals("테스트 음성입니다", result);
    }

    @Test
    void recognize_NullAudioFile_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            speechToTextService.recognize(null, "ko");
        });
    }

    @Test
    void recognize_EmptyAudioFile_ThrowsException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "audio", 
            "empty.mp3", 
            "audio/mpeg", 
            new byte[0]
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            speechToTextService.recognize(emptyFile, "ko");
        });
    }
}
