package com.melog.melog.emotion.adapter.in.web;

import com.melog.melog.emotion.domain.model.request.*;
import com.melog.melog.emotion.domain.model.response.*;
import com.melog.melog.emotion.application.port.in.EmotionRecordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/users/{nickname}/emotions")
@RequiredArgsConstructor
public class EmotionRecordController {

    private final EmotionRecordUseCase emotionRecordUseCase;

    /**
     * 감정 등록 및 분석 요청 (텍스트 방식)
     * POST /api/users/{nickname}/emotions
     */
    @PostMapping
    public ResponseEntity<EmotionRecordResponse> createEmotionRecord(@PathVariable String nickname,
                                                                   @RequestBody EmotionRecordCreateRequest request) {
        EmotionRecordResponse response = emotionRecordUseCase.createEmotionRecord(nickname, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 감정 등록 및 분석 요청 (음성 파일 방식)
     * POST /api/users/{nickname}/emotions (multipart/form-data)
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<EmotionRecordResponse> createEmotionRecordWithAudio(@PathVariable String nickname,
                                                                            @RequestParam("audioFile") MultipartFile audioFile,
                                                                            @RequestParam("userSelectedEmotion") String userSelectedEmotionJson) {
        // TODO: 음성 파일 처리 및 STT 변환 로직 구현
        // TODO: JSON 파싱하여 EmotionRecordCreateRequest 생성
        throw new UnsupportedOperationException("음성 파일 처리는 아직 구현되지 않았습니다.");
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
} 