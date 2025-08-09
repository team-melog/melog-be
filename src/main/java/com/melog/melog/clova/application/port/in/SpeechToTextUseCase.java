package com.melog.melog.clova.application.port.in;

import org.springframework.web.multipart.MultipartFile;

import com.melog.melog.clova.domain.model.response.SttResponse;

public interface SpeechToTextUseCase {

    SttResponse recognize(MultipartFile audio, String language);
    
}
