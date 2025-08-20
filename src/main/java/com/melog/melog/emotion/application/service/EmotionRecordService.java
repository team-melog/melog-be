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

import java.time.LocalDate;
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

    /**
     * 감정 등록 및 분석 요청 (텍스트) - 날짜 지정 가능 (Admin 전용)
     */
    @Transactional
    public EmotionRecordResponse createEmotionRecordWithDate(String nickname, EmotionRecordCreateRequest request, LocalDate targetDate) {
        var savedRecord = emotionRecordCreationService.createEmotionRecordFromTextWithDate(nickname, request, targetDate);
        return emotionRecordQueryService.getEmotionRecord(nickname, savedRecord.getId());
    }

    @Override
    @Transactional
    public EmotionRecordResponse createEmotionRecordWithAudio(String nickname, MultipartFile audioFile, String userSelectedEmotionJson) {
        try {
            // 🔍 STT 데이터 흐름 추적 시작
            log.info("[EMOTION SERVICE] ===== STT 데이터 흐름 추적 시작 =====");
            log.info("[EMOTION SERVICE] 1. STT 호출 전 - audioFile: {}, size: {} bytes", 
                    audioFile.getOriginalFilename(), audioFile.getSize());
            
            // STT를 통해 음성을 텍스트로 변환
            String text = speechToTextUseCase.recognizeToText(audioFile, "ko-KR");
            
            // 🔍 STT 결과 상세 분석
            log.info("[EMOTION SERVICE] 2. STT 호출 완료");
            log.info("[EMOTION SERVICE] 3. 반환된 text 객체: '{}'", text);
            log.info("[EMOTION SERVICE] 4. text 객체 타입: {}", text != null ? text.getClass().getSimpleName() : "NULL");
            log.info("[EMOTION SERVICE] 5. text가 null인가? {}", text == null);
            log.info("[EMOTION SERVICE] 6. text 길이: {}", text != null ? text.length() : "N/A");
            log.info("[EMOTION SERVICE] 7. text가 빈 문자열인가? {}", text != null ? text.isEmpty() : "N/A");
            log.info("[EMOTION SERVICE] 8. text가 공백만 있는가? {}", text != null ? text.trim().isEmpty() : "N/A");
            
            // 🔍 STT 결과 유효성 검증
            if (text == null || text.trim().isEmpty()) {
                log.error("[EMOTION SERVICE] STT 변환 실패: text가 null이거나 빈 문자열입니다. text='{}'", text);
                throw new RuntimeException("음성을 텍스트로 변환할 수 없습니다. 다시 녹음해주세요.");
            }
            
            // 텍스트 길이 검증 (너무 짧은 경우)
            if (text.trim().length() < 3) {
                log.error("[EMOTION SERVICE] STT 변환 결과가 너무 짧습니다: text='{}', length={}", text, text.trim().length());
                throw new RuntimeException("음성 인식 결과가 너무 짧습니다. 더 명확하게 말씀해주세요.");
            }
            
            log.info("[EMOTION SERVICE] STT 결과 유효성 검증 통과: text='{}', length={}", text, text.trim().length());
            
            // 기존 로그 유지
            log.info("STT 변환 결과: {}", text);
            
            // 🔍 감정 기록 생성으로 전달
            log.info("[EMOTION SERVICE] 9. 감정 기록 생성 서비스 호출 시작");
            log.info("[EMOTION SERVICE] 10. 전달할 text: '{}'", text);
            
            // 음성 파일 기반 감정 기록 생성
            var savedRecord = emotionRecordCreationService.createEmotionRecordFromAudio(nickname, text, userSelectedEmotionJson, audioFile);
            
            log.info("[EMOTION SERVICE] 11. 감정 기록 생성 완료 - ID: {}", savedRecord.getId());
            log.info("[EMOTION SERVICE] ===== STT 데이터 흐름 추적 완료 =====");
            
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