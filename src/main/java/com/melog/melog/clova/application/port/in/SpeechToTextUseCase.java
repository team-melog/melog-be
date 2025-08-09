package com.melog.melog.clova.application.port.in;

import org.springframework.web.multipart.MultipartFile;

import com.melog.melog.clova.domain.model.response.SttResponse;

public interface SpeechToTextUseCase {

    SttResponse recognize(MultipartFile audio, String language);
    
    /**
     * 음성 파일을 텍스트로 변환 (간단 버전)
     */
    String recognizeToText(MultipartFile audio, String language);
    
}
