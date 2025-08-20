package com.melog.melog.common.adapter.in.web;

import com.melog.melog.common.service.DummyDataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 더미 데이터 초기화 Controller (간단 버전)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DummyDataController {

    private final DummyDataService dummyDataService;

    /**
     * 더미 데이터 일괄 생성 (유저 + 감정기록)
     * POST /api/admin/init-dummy-data
     */
    @PostMapping("/init-dummy-data")
    public ResponseEntity<String> initializeDummyData() {
        try {
            dummyDataService.initializeAllDummyData();
            return ResponseEntity.ok("더미 데이터 초기화 완료");
        } catch (Exception e) {
            log.error("더미 데이터 초기화 실패", e);
            return ResponseEntity.internalServerError().body("초기화 실패: " + e.getMessage());
        }
    }
}