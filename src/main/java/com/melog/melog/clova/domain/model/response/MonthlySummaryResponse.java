package com.melog.melog.clova.domain.model.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlySummaryResponse {
    private String summary; // 월별 요약
    private String advice; // 한줄 조언
}
