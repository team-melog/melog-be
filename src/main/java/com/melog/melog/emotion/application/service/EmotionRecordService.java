package com.melog.melog.emotion.application.service;

import com.melog.melog.emotion.application.port.in.EmotionRecordUseCase;
import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.melog.melog.clova.application.port.in.SpeechToTextUseCase;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionRecordService implements EmotionRecordUseCase {

    private final EmotionRecordCreationService emotionRecordCreationService;
    private final EmotionRecordQueryService emotionRecordQueryService;
    private final EmotionRecordManagementService emotionRecordManagementService;
    private final EmotionInsightService emotionInsightService;
    private final SpeechToTextUseCase speechToTextUseCase;

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecord(String nickname, EmotionRecordCreateRequest request) {
        var savedRecord = emotionRecordCreationService.createEmotionRecordFromText(nickname, request);
        return emotionRecordQueryService.getEmotionRecord(nickname, savedRecord.getId());
    }

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecordWithAudio(String nickname, MultipartFile audioFile, String userSelectedEmotionJson) {
        try {
            // STT를 통해 음성을 텍스트로 변환
            String text = speechToTextUseCase.recognizeToText(audioFile, "ko-KR");
            log.info("STT 변환 결과: {}", text);
            
            // 음성 파일 기반 감정 기록 생성
            var savedRecord = emotionRecordCreationService.createEmotionRecordFromAudio(nickname, text, userSelectedEmotionJson, audioFile);
            return emotionRecordQueryService.getEmotionRecord(nickname, savedRecord.getId());
            
        } catch (Exception e) {
            log.error("음성 파일 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("음성 파일 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public EmotionRecordResponse updateEmotionSelection(String nickname, Long recordId, EmotionRecordSelectRequest request) {
        return emotionRecordManagementService.updateEmotionSelection(nickname, recordId, request);
    }

    @Override
    @Transactional
    public EmotionRecordResponse updateEmotionText(String nickname, Long recordId, EmotionRecordTextUpdateRequest request) {
        return emotionRecordManagementService.updateEmotionText(nickname, recordId, request);
    }

    @Override
    public EmotionRecordResponse getEmotionRecord(String nickname, Long recordId) {
        return emotionRecordQueryService.getEmotionRecord(nickname, recordId);
    }

    @Override
    @Transactional
    public void deleteEmotionRecord(String nickname, Long recordId) {
        emotionRecordManagementService.deleteEmotionRecord(nickname, recordId);
    }

    @Override
    public List<EmotionCalendarResponse> getEmotionCalendar(String nickname, YearMonth month) {
        return emotionRecordQueryService.getEmotionCalendar(nickname, month);
    }

    @Override
    public EmotionChartResponse getEmotionChart(String nickname, YearMonth month) {
        return emotionRecordQueryService.getEmotionChart(nickname, month);
    }

    @Override
    public EmotionInsightResponse getEmotionInsight(String nickname, YearMonth month) {
        return emotionInsightService.getEmotionInsight(nickname, month);
    }

    @Override
    public EmotionListResponse getEmotionList(String nickname, int page, int size) {
        return emotionRecordQueryService.getEmotionList(nickname, page, size);
    }
}