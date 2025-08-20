package com.melog.melog.emotion.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 감정 기록 입력 유효성 검증을 위한 유틸리티 클래스
 * 음성 파일은 기존 검증 로직 사용, 텍스트 검증에만 집중
 * 
 * 🎯 안전한 검증 방식 (제출 안전성 확보)
 * ==========================================
 * 핵심 아이디어: 의미 없는 문자 비율 40% 이상인 경우만 차단
 * 
 * 검증 규칙:
 * 1. 자음/모음만 있는 경우 차단
 * 2. 특수문자와 공백만 있는 경우 차단  
 * 3. 5자 이하인 경우 차단
 * 4. 한글 중간에 자음/모음이 섞인 경우 차단
 * 5. 의미 없는 문자 비율 20% 이상인 경우 차단 (40% → 20%로 강화)
 * 
 * 제거된 규칙 (안정성 고려):
 * - 감정 표현 반복 차단 (사용자 진짜 감정 차단 위험)
 * - 일상적 내용 키워드 검증 (STT 정확도 문제)
 * 
 * 장점: 일반 사용자의 정상적인 텍스트는 대부분 통과
 * ==========================================
 */
@Slf4j
public class InputValidationUtil {

    // 의미 없는 텍스트 패턴 (자음/모음만 있는 경우, 특수문자 반복 등)
    private static final Pattern MEANINGLESS_TEXT_PATTERN = Pattern.compile(
        "^[ㄱ-ㅎㅏ-ㅣ\\s\\p{Punct}]*$|" +  // 자음/모음만 있는 경우
        "^[\\p{Punct}\\s]*$|" +            // 특수문자와 공백만 있는 경우
        "^.{1,5}$|" +                      // 5자 이하인 경우
        "^[가-힣]*[ㄱ-ㅎㅏ-ㅣ]+[가-힣]*$"  // 한글 중간에 자음/모음이 섞인 경우
    );
    
    // 의미 없는 문자 비율 임계값 (20% 이상이면 차단)
    private static final double MEANINGLESS_CHAR_THRESHOLD = 0.2;

    /**
     * 텍스트 입력의 유효성을 검증합니다.
     * 
     * @param text 검증할 텍스트
     * @return 유효한 텍스트인 경우 true, 의미 없는 텍스트인 경우 false
     * 
     * 🎯 안전한 검증 방식: 의미 없는 문자 비율 40% 이상인 경우만 차단
     * 일반 사용자의 정상적인 텍스트는 대부분 통과하도록 설계
     */
    public static boolean isValidText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String trimmedText = text.trim();
        
        // 길이 검증 (너무 짧은 경우만 차단)
        if (trimmedText.length() < 5) {  // 5자로 완화
            log.warn("텍스트가 너무 짧습니다: {} (길이: {})", trimmedText, trimmedText.length());
            return false;
        }

        // 극단적인 의미 없는 텍스트 패턴만 차단
        if (MEANINGLESS_TEXT_PATTERN.matcher(trimmedText).matches()) {
            log.warn("극단적인 의미 없는 텍스트 패턴 감지: {}", trimmedText);
            return false;
        }

        // 의미 없는 문자 비율 계산 (40% 이상이면 차단)
        int meaninglessCharCount = countMeaninglessCharacters(trimmedText);
        double meaninglessRatio = (double) meaninglessCharCount / trimmedText.length();
        
        if (meaninglessRatio >= MEANINGLESS_CHAR_THRESHOLD) {
            log.warn("의미 없는 문자 비율이 너무 높습니다: {} (비율: {:.2f})", trimmedText, meaninglessRatio);
            return false;
        }

        log.info("텍스트 유효성 검증 통과: {} (길이: {}, 의미없는 문자 비율: {:.2f})", 
                trimmedText, trimmedText.length(), meaninglessRatio);
        return true;
    }

    /**
     * 의미 없는 문자(자음/모음, 특수문자, 공백)의 개수를 계산합니다.
     */
    private static int countMeaninglessCharacters(String text) {
        return (int) text.chars()
                .mapToObj(ch -> (char) ch)
                .filter(ch -> isMeaninglessCharacter(ch))
                .count();
    }
    
    /**
     * 개별 문자가 의미 없는 문자인지 판단합니다.
     */
    private static boolean isMeaninglessCharacter(char ch) {
        // 자음/모음
        if ((ch >= 0x1100 && ch <= 0x11FF) || (ch >= 0x3130 && ch <= 0x318F)) {
            return true;
        }
        
        // 특수문자와 공백
        if (Character.isWhitespace(ch) || !Character.isLetterOrDigit(ch)) {
            return true;
        }
        
        return false;
    }

    /**
     * 텍스트가 감정 분석에 적합한지 검증합니다.
     * 
     * @param text 검증할 텍스트
     * @return 감정 분석 가능한 텍스트인 경우 true
     * 
     * 🎯 안전한 검증 방식: 기본 텍스트 검증만 수행
     * STT 정확도 문제를 고려하여 키워드 기반 검증은 제거
     */
    public static boolean isEmotionAnalysisSuitable(String text) {
        // 기본 텍스트 유효성만 검증
        if (!isValidText(text)) {
            return false;
        }

        // STT 정확도 문제를 고려하여 키워드 기반 검증 제거
        // 대신 기본 텍스트 검증만으로 충분하다고 판단
        
        log.info("감정 분석 적합성 검증 통과: {}", text);
        return true;
    }
}
