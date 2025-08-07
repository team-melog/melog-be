package com.melog.melog.application.port.in;

import com.melog.melog.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.domain.model.request.EmotionRecordUpdateRequest;
import com.melog.melog.domain.model.response.EmotionRecordResponse;

import java.time.LocalDate;
import java.util.List;

public interface EmotionRecordUseCase {
    
    /**
     * 감정 기록 생성
     */
    EmotionRecordResponse createEmotionRecord(Long userId, EmotionRecordCreateRequest request);
    
    /**
     * 감정 기록 조회
     */
    EmotionRecordResponse getEmotionRecord(Long recordId);
    
    /**
     * 사용자의 감정 기록 목록 조회
     */
    List<EmotionRecordResponse> getEmotionRecordsByUser(Long userId);
    
    /**
     * 사용자의 특정 날짜 감정 기록 조회
     */
    EmotionRecordResponse getEmotionRecordByUserAndDate(Long userId, LocalDate date);
    
    /**
     * 감정 기록 수정
     */
    EmotionRecordResponse updateEmotionRecord(Long recordId, EmotionRecordUpdateRequest request);
    
    /**
     * 감정 기록 삭제
     */
    void deleteEmotionRecord(Long recordId);
} 