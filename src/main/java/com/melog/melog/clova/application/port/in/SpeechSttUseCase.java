package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.SpeechSttRequest;
import com.melog.melog.clova.domain.model.response.SpeechSttResponse;

public interface SpeechSttUseCase {

    SpeechSttResponse execute(SpeechSttRequest request);

}
