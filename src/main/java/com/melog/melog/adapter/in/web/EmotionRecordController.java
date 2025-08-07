package com.melog.melog.adapter.in.web;

import com.melog.melog.application.port.in.EmotionRecordUseCase;
import com.melog.melog.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.domain.model.request.EmotionRecordUpdateRequest;
import com.melog.melog.domain.model.response.EmotionRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/emotion-records")
@RequiredArgsConstructor
public class EmotionRecordController {

    private final EmotionRecordUseCase emotionRecordUseCase;

    /**
     * 감정 기록 생성
     * POST /api/emotion-records
     */
    @PostMapping
    public ResponseEntity<EmotionRecordResponse> createEmotionRecord(
            @RequestParam Long userId,
            @RequestBody EmotionRecordCreateRequest request) {
        EmotionRecordResponse response = emotionRecordUseCase.createEmotionRecord(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 감정 기록 조회
     * GET /api/emotion-records/{recordId}
     */
    @GetMapping("/{recordId}")
    public ResponseEntity<EmotionRecordResponse> getEmotionRecord(@PathVariable Long recordId) {
        EmotionRecordResponse response = emotionRecordUseCase.getEmotionRecord(recordId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 감정 기록 목록 조회
     * GET /api/emotion-records/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EmotionRecordResponse>> getEmotionRecordsByUser(@PathVariable Long userId) {
        List<EmotionRecordResponse> responses = emotionRecordUseCase.getEmotionRecordsByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 사용자의 특정 날짜 감정 기록 조회
     * GET /api/emotion-records/user/{userId}/date/{date}
     */
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<EmotionRecordResponse> getEmotionRecordByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        EmotionRecordResponse response = emotionRecordUseCase.getEmotionRecordByUserAndDate(userId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 기록 수정
     * PUT /api/emotion-records/{recordId}
     */
    @PutMapping("/{recordId}")
    public ResponseEntity<EmotionRecordResponse> updateEmotionRecord(
            @PathVariable Long recordId,
            @RequestBody EmotionRecordUpdateRequest request) {
        EmotionRecordResponse response = emotionRecordUseCase.updateEmotionRecord(recordId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 감정 기록 삭제
     * DELETE /api/emotion-records/{recordId}
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteEmotionRecord(@PathVariable Long recordId) {
        emotionRecordUseCase.deleteEmotionRecord(recordId);
        return ResponseEntity.noContent().build();
    }
} 