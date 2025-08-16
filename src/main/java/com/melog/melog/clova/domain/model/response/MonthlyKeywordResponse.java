package com.melog.melog.clova.domain.model.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyKeywordResponse {
    private String keyword; // 키워드
    private Integer weight; // 중요도 (1-100)
}
