package com.melog.melog.emotion.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.application.port.in.EmotionRecordUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users/{nickname}/emotions")
@RequiredArgsConstructor
public class EmotionRecordController {

    private final EmotionRecordUseCase emotionRecordUseCase;
    private final ObjectMapper objectMapper;

    /**
     * 감정 등록 및 분석 요청 (STT - 음성 파일)
     * POST /api/users/{nickname}/emotions/stt
     * 
     * Postman 설정 방법:
     * 1. Body 탭에서 "form-data" 선택
     * 2. Key: "audioFile" (File 타입으로 설정)
     * 3. Value: 음성 파일 선택
     * 4. Key: "userSelectedEmotion" (Text 타입으로 설정, 선택사항)
     * 5. Value: JSON 문자열 (예: {"type":"JOY","step":3})
     */
    @PostMapping("/stt")
    public ResponseEntity<EmotionRecordResponse> createEmotionRecordWithSTT(
            @PathVariable String nickname,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "userSelectedEmotion", required = false) String userSelectedEmotionJson) {
        
        log.info("STT 요청 수신 - nickname: {}, audioFile: {}, userSelectedEmotion: {}", 
                nickname, 
                audioFile != null ? audioFile.getOriginalFilename() : "null",
                userSelectedEmotionJson);
        
        if (audioFile == null || audioFile.isEmpty()) {
            log.error("음성 파일이 제공되지 않음 - nickname: {}", nickname);
            throw new IllegalArgumentException("음성 파일은 필수입니다. audioFile 파라미터를 확인해주세요.");
        }
        
        log.info("음성 파일 처리 시작 - filename: {}, size: {} bytes", 
                audioFile.getOriginalFilename(), audioFile.getSize());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emotionRecordUseCase.createEmotionRecordWithAudio(nickname, audioFile, userSelectedEmotionJson));
    }

    /**
     * 감정 등록 및 분석 요청 (텍스트)
     * POST /api/users/{nickname}/emotions/text
     */
    @PostMapping("/text")
    public ResponseEntity<EmotionRecordResponse> createEmotionRecordWithText(
            @PathVariable String nickname,
            @RequestBody EmotionRecordCreateRequest request) {
        
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("텍스트는 필수입니다.");
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emotionRecordUseCase.createEmotionRecord(nickname, request));
    }

    /**
     * 월별 캘린더 감정 리스트 조회
     * GET /api/users/{nickname}/emotions/calendar?month=YYYY-MM
     */
    @GetMapping("/calendar")
    public ResponseEntity<List<EmotionCalendarResponse>> getEmotionCalendar(@PathVariable String nickname,
                                                                          @RequestParam String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        List<EmotionCalendarResponse> response = emotionRecordUseCase.getEmotionCalendar(nickname, yearMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 월별 감정 분포 통계 (차트)
     * GET /api/users/{nickname}/emotions/summary/chart?month=YYYY-MM
     */
    @GetMapping("/summary/chart")
    public ResponseEntity<EmotionChartResponse> getEmotionChart(@PathVariable String nickname,
                                                              @RequestParam String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        EmotionChartResponse response = emotionRecordUseCase.getEmotionChart(nickname, yearMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 월별 키워드 및 한줄 요약
     * GET /api/users/{nickname}/emotions/summary/insight?month=YYYY-MM
     */
    @GetMapping("/summary/insight")
    public ResponseEntity<EmotionInsightResponse> getEmotionInsight(@PathVariable String nickname,
                                                                  @RequestParam String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        EmotionInsightResponse response = emotionRecordUseCase.getEmotionInsight(nickname, yearMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 기록 리스트 조회 (홈탭 및 무한스크롤용)
     * GET /api/users/{nickname}/emotions?page=0&size=7
     */
    @GetMapping
    public ResponseEntity<EmotionListResponse> getEmotionList(@PathVariable String nickname,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "7") int size) {
        EmotionListResponse response = emotionRecordUseCase.getEmotionList(nickname, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 등록 및 분석 요청 (통합 엔드포인트 - 하위 호환성)
     * POST /api/users/{nickname}/emotions
     * 
     * Content-Type에 따라 자동으로 적절한 메서드로 라우팅
     */
    @PostMapping
    public ResponseEntity<EmotionRecordResponse> createEmotionRecord(
            @PathVariable String nickname,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "userSelectedEmotion", required = false) String userSelectedEmotionJson,
            @RequestBody(required = false) EmotionRecordCreateRequest request) {
        
        // STT 요청인 경우 (audioFile이 제공된 경우)
        if (audioFile != null && !audioFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(emotionRecordUseCase.createEmotionRecordWithAudio(nickname, audioFile, userSelectedEmotionJson));
        }
        
        // 텍스트 요청인 경우 (JSON 바디가 제공된 경우)
        if (request != null && request.getText() != null && !request.getText().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(emotionRecordUseCase.createEmotionRecord(nickname, request));
        }
        
        // 둘 다 제공되지 않은 경우
        throw new IllegalArgumentException("음성 파일 또는 텍스트 중 하나는 필수입니다.");
    }

    /**
     * 감정 수정(최종 선택 확정)
     * PUT /api/users/{nickname}/emotions/{id}/select
     */
    @PutMapping("/{id}/select")
    public ResponseEntity<EmotionRecordResponse> updateEmotionSelection(@PathVariable String nickname,
                                                                      @PathVariable Long id,
                                                                      @RequestBody EmotionRecordSelectRequest request) {
        EmotionRecordResponse response = emotionRecordUseCase.updateEmotionSelection(nickname, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 수정(텍스트)
     * PUT /api/users/{nickname}/emotions/{id}/text
     */
    @PutMapping("/{id}/text")
    public ResponseEntity<EmotionRecordResponse> updateEmotionText(@PathVariable String nickname,
                                                                 @PathVariable Long id,
                                                                 @RequestBody EmotionRecordTextUpdateRequest request) {
        EmotionRecordResponse response = emotionRecordUseCase.updateEmotionText(nickname, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 상세 조회
     * GET /api/users/{nickname}/emotions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmotionRecordResponse> getEmotionRecord(@PathVariable String nickname,
                                                                @PathVariable Long id) {
        EmotionRecordResponse response = emotionRecordUseCase.getEmotionRecord(nickname, id);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 기록 삭제
     * DELETE /api/users/{nickname}/emotions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmotionRecord(@PathVariable String nickname,
                                                  @PathVariable Long id) {
        emotionRecordUseCase.deleteEmotionRecord(nickname, id);
        return ResponseEntity.noContent().build();
    }
} 