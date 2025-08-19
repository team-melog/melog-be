package com.melog.melog.clova.domain.model;

import java.util.List;

import com.melog.melog.emotion.domain.EmotionType;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest.UserSelectedEmotion;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class VoiceToner {

    private int volume;           // 고정 0
    private int speed;            // -1..1 (음수=조금 빠름, 양수=조금 느림)
    private int pitch;            // -2..2 (음수=더 높게, 양수=더 낮게)
    private int alpha;            // 고정 0 (톤 변화가 커서 미적용)
    private int emotion;          // 0..3 (0=중립, 1=슬픔/지침, 2=기쁨, 3=분노)
    private int emotionStrength;  // 0..2 (약/보통/강)

    private static final int SPD_MIN = -1, SPD_MAX = 1;
    private static final int PIT_MIN = -2, PIT_MAX = 2;
    private static final int SPEED_THRESHOLD = 50; // 50% 이상일 때만 speed 1스텝

    public static VoiceToner toneFromEmotions(List<UserSelectedEmotion> selections) {
        int s = 0, p = 0; // volume=0, alpha=0 고정
        int chosenEmotion = 0, chosenPercent = -1;
        EmotionType chosenType = null;

        if (selections != null) {
            for (UserSelectedEmotion sel : selections) {
                if (sel == null || sel.getEmotionType() == null) continue;
                EmotionType type = sel.getEmotionType();
                int percent = normalizePercent(sel.getPercentage());

                switch (type) {
                    case JOY -> {          // 빠르게/높게
                        s += -scale01(percent);    // 0 or -1
                        p +=  scale(percent, -2);  // 0..-2
                    }
                    case EXCITEMENT -> {   // 기쁨과 동일 상한
                        s += -scale01(percent);
                        p +=  scale(percent, -2);  // 0..-2
                    }
                    case CALMNESS -> {     // 정상 속도, 낮게
                        // s += 0;
                        p +=  scale(percent, +2);  // 0..+2
                    }
                    case ANGER -> {        // 약간 빠르고/낮게
                        s += -scale01(percent);
                        p +=  scale(percent, +1);  // 0..+1
                    }
                    case SADNESS -> {      // 속도 미적용, 낮게
                        // s += 0;
                        p +=  scale(percent, +2);  // 0..+2
                    }
                    case GUIDANCE -> {     // 50%↑이면 느리게(+1), pitch 변화 없음
                        s +=  scale01(percent);    // 0 or +1
                    }
                }

                // JOY/EXCITEMENT는 emotion=0(중립), CALMNESS도 0
                int mapped = mapEmotion(type);
                if (mapped != 0 && percent > chosenPercent) {
                    chosenEmotion = mapped;
                    chosenPercent = percent;
                    chosenType = type;
                }
            }
        }

        // strength 계산: GUIDANCE는 한 단계 약하게(최대 1)
        int strength;
        if (chosenEmotion == 0) {
            strength = 0;
        } else if (chosenType == EmotionType.GUIDANCE) {
            strength = mapStrengthGuidance(chosenPercent); // 0..1
        } else {
            strength = mapStrength(chosenPercent);         // 0..2
        }

        return VoiceToner.builder()
                .volume(0)
                .speed (clamp(s, SPD_MIN, SPD_MAX))
                .pitch (clamp(p, PIT_MIN, PIT_MAX))
                .alpha (0)
                .emotion(chosenEmotion)
                .emotionStrength(strength)
                .build();
    }

    /* ===== 유틸 ===== */

    private static int scale01(int percent) {
        return (percent >= SPEED_THRESHOLD) ? 1 : 0;
    }

    private static int scale(int percent, int maxDeltaAbs) {
        int sign = Integer.signum(maxDeltaAbs);
        int mag  = Math.abs(maxDeltaAbs);
        int val  = Math.round(mag * (percent / 100.0f));
        return sign * val;
    }

    private static int mapEmotion(EmotionType type) {
        return switch (type) {
            case JOY, EXCITEMENT -> 0; // 의도적으로 중립 처리
            case SADNESS, GUIDANCE -> 1; // ★ 지침도 1로 전송
            case ANGER -> 3;
            default -> 0; // CALMNESS → 중립
        };
    }

    private static int mapStrength(int percent) {
        if (percent <= 33) return 0;
        if (percent <= 66) return 1;
        return 2;
    }

    // 지침은 한 단계 약하게: 기본 강도에서 -1, 최저 0 (0..1 범위)
    private static int mapStrengthGuidance(int percent) {
        return Math.max(0, mapStrength(percent) - 1);
    }

    private static int normalizePercent(Integer percentage) {
        if (percentage == null) return 0;
        return Math.max(0, Math.min(100, percentage));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * 기본 음성 톤 설정 반환
     * 모든 값이 중립(0)으로 설정된 기본 톤
     * 
     * @return 기본 음성 톤 설정
     */
    public static VoiceToner getDefaultTone() {
        return VoiceToner.builder()
                .volume(0)
                .speed(0)
                .pitch(0)
                .alpha(0)
                .emotion(0)
                .emotionStrength(0)
                .build();
    }
}
