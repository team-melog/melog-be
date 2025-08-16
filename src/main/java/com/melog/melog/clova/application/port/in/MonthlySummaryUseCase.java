package com.melog.melog.clova.application.port.in;

import com.melog.melog.clova.domain.model.request.MonthlySummaryRequest;
import com.melog.melog.clova.domain.model.response.MonthlySummaryResponse;

public interface MonthlySummaryUseCase {
    
    /**
     * 월별 감정 기록을 분석하여 키워드와 요약을 생성합니다.
     */
    MonthlySummaryResponse generateMonthlySummary(MonthlySummaryRequest request);
}
