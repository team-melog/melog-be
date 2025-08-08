package com.melog.melog.clova.application.port.in;

import org.springframework.web.multipart.MultipartFile;

import com.melog.melog.clova.domain.model.request.SpeechToTextRequest;
import com.melog.melog.clova.domain.model.response.ClovaSttResponse;
import com.melog.melog.clova.domain.model.response.SpeechToTextResponse;

public interface SpeechSttUseCase {

    SpeechToTextResponse execute(SpeechToTextRequest request);
    
    ClovaSttResponse recognize(MultipartFile audio, String language);
    
    ClovaSttResponse recognizeShortSentence(MultipartFile audio, String language);

}
