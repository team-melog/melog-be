package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.AnalyzeSentimentRequest;
import com.melog.melog.clova.domain.model.response.AnalyzeSentimentResponse;

public interface AnalyzeSentimentUseCase {

    AnalyzeSentimentResponse execute(AnalyzeSentimentRequest request);
    
}
