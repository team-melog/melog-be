package com.melog.melog.emotion.application.port.in;

import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.emotion.domain.model.request.EmotionRecordSelectRequest;
import com.melog.melog.emotion.domain.model.request.EmotionRecordTextUpdateRequest;
import com.melog.melog.emotion.domain.model.response.EmotionRecordResponse;
import com.melog.melog.emotion.domain.model.response.EmotionCalendarResponse;
import com.melog.melog.emotion.domain.model.response.EmotionChartResponse;
import com.melog.melog.emotion.domain.model.response.EmotionInsightResponse;
import com.melog.melog.emotion.domain.model.response.EmotionListResponse;

import java.time.YearMonth;
import java.util.List;

public interface EmotionRecordUseCase {
    
    /**
     * 감정 등록 및 분석 요청
     */
    EmotionRecordResponse createEmotionRecord(String nickname, EmotionRecordCreateRequest request);
    
    /**
     * 감정 수정(최종 선택 확정)
     */
    EmotionRecordResponse updateEmotionSelection(String nickname, Long recordId, EmotionRecordSelectRequest request);
    
    /**
     * 감정 수정(텍스트)
     */
    EmotionRecordResponse updateEmotionText(String nickname, Long recordId, EmotionRecordTextUpdateRequest request);
    
    /**
     * 감정 상세 조회
     */
    EmotionRecordResponse getEmotionRecord(String nickname, Long recordId);
    
    /**
     * 감정 기록 삭제
     */
    void deleteEmotionRecord(String nickname, Long recordId);
    
    /**
     * 월별 캘린더 감정 리스트 조회
     */
    List<EmotionCalendarResponse> getEmotionCalendar(String nickname, YearMonth month);
    
    /**
     * 월별 감정 분포 통계 (차트)
     */
    EmotionChartResponse getEmotionChart(String nickname, YearMonth month);
    
    /**
     * 월별 키워드 및 한줄 요약
     */
    EmotionInsightResponse getEmotionInsight(String nickname, YearMonth month);
    
    /**
     * 감정 기록 리스트 조회 (페이징)
     */
    EmotionListResponse getEmotionList(String nickname, int page, int size);
} 