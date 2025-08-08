package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.SpeechToTextRequest;
import com.melog.melog.clova.domain.model.response.SpeechToTextResponse;

public interface SpeechSttUseCase {

    SpeechToTextResponse execute(SpeechToTextRequest request);

}
