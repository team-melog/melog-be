package com.melog.melog.emotion.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melog.melog.clova.application.port.in.TextToSpeakUseCase;
import com.melog.melog.clova.domain.model.VoiceToner;
import com.melog.melog.clova.domain.model.VoiceType;
import com.melog.melog.clova.domain.model.request.TtsRequest;
import com.melog.melog.clova.domain.model.response.TtsResponse;
import com.melog.melog.common.service.S3FileService;
import com.melog.melog.emotion.application.port.in.AudioUseCase;
import com.melog.melog.emotion.application.port.out.EmotionCommentPersistencePort;
import com.melog.melog.emotion.application.port.out.EmotionRecordPersistencePort;
import com.melog.melog.emotion.application.port.out.EmotionScorePersistencePort;
import com.melog.melog.emotion.application.port.out.TtsCachePersistencePort;
import com.melog.melog.emotion.application.port.out.UserSelectedEmotionPersistencePort;
import com.melog.melog.emotion.domain.EmotionComment;
import com.melog.melog.emotion.domain.EmotionRecord;
import com.melog.melog.emotion.domain.EmotionScore;
import com.melog.melog.emotion.domain.TtsCache;
import com.melog.melog.emotion.domain.UserSelectedEmotion;
import com.melog.melog.emotion.domain.model.request.AudioRequest;
import com.melog.melog.emotion.domain.model.request.EmotionRecordCreateRequest;
import com.melog.melog.emotion.domain.model.response.AudioResponse;
import com.melog.melog.user.application.port.out.UserPersistencePort;
import com.melog.melog.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 오디오 처리 서비스
 * 
 * 오디오 파일 조회 및 TTS 생성에 관한 핵심 비즈니스 로직을 담당하는 서비스입니다.
 * 사용자 업로드 파일과 TTS 생성 파일을 통합적으로 관리하며,
 * TTS 캐시를 통한 성능 최적화를 제공합니다.
 * 
 * 주요 책임:
 * - 사용자 업로드 오디오 파일 조회
 * - TTS 오디오 생성 및 캐시 관리
 * - 감정 데이터 기반 음성 톤 계산
 * - 캐시 키 생성 및 관리
 * 
 * 비즈니스 규칙:
 * - 동일한 텍스트와 음성 설정에 대해서는 캐시된 파일 우선 반환
 * - 감정 데이터는 UserSelectedEmotion을 우선하고, 없으면 EmotionScore 상위 3개 사용
 * - TTS 생성 실패 시에도 적절한 오류 메시지 제공
 * 
 * @author Melog Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioService implements AudioUseCase {

    // 의존성 주입: 포트들
    private final UserPersistencePort userPersistencePort;
    private final EmotionRecordPersistencePort emotionRecordPersistencePort;
    private final EmotionScorePersistencePort emotionScorePersistencePort;
    private final UserSelectedEmotionPersistencePort userSelectedEmotionPersistencePort;
    private final EmotionCommentPersistencePort emotionCommentPersistencePort;
    private final TtsCachePersistencePort ttsCachePersistencePort;
    
    // 의존성 주입: 외부 서비스들
    private final TextToSpeakUseCase textToSpeakUseCase;
    private final S3FileService s3FileService;
    private final ObjectMapper objectMapper;

    // 상수 정의
    private static final String DEFAULT_TTS_FORMAT = "wav";
    private static final String DEFAULT_TTS_MIME_TYPE = "audio/wav";
    private static final int MAX_EMOTION_COUNT_FOR_TTS = 3; // TTS 생성 시 사용할 최대 감정 개수

    /**
     * 오디오 파일 조회 또는 생성
     * 
     * 요청 타입에 따라 사용자 업로드 파일을 반환하거나 TTS로 새로운 오디오를 생성합니다.
     * TTS 요청의 경우 기존 캐시를 우선 확인하고, 없을 경우에만 새로 생성합니다.
     * 
     * 처리 흐름:
     * 1. 요청 파라미터 검증
     * 2. 사용자 및 감정 기록 존재 여부 확인
     * 3. 요청 타입에 따른 분기 처리
     * 4. 결과 응답 구성
     * 
     * @param request 오디오 요청 정보
     * @return 오디오 파일 정보 및 메타데이터
     */
    @Override
    @Transactional
    public AudioResponse getOrCreateAudio(AudioRequest request) {
        log.info("오디오 조회/생성 요청 시작: nickname={}, recordId={}, isRequiredUserAudio={}, voiceType={}", 
                request.getNickname(), request.getRecordId(), request.getIsRequiredUserAudio(), request.getVoiceType());
        
        try {
            // 1. 요청 파라미터 유효성 검증
            validateAudioRequest(request);
            
            // 2. 사용자 및 감정 기록 조회
            EmotionRecord emotionRecord = findEmotionRecordWithValidation(request.getNickname(), request.getRecordId());
            
            // 3. 요청 타입에 따른 분기 처리
            AudioResponse response;
            if (request.isUserUploadRequest()) {
                // 사용자 업로드 파일 또는 감정 기반 TTS 파일 반환
                response = handleUserUploadAudio(emotionRecord, request.getVoiceType());
                log.info("사용자 업로드 오디오 반환 완료: recordId={}, voiceType={}", request.getRecordId(), request.getVoiceType());
            } else {
                // voiceType이 null이 아닐 때만 TTS 생성 또는 캐시된 파일 반환
                if (request.getVoiceType() != null) {
                    response = handleTtsAudio(emotionRecord, request.getVoiceType());
                    log.info("TTS 오디오 처리 완료: recordId={}, voiceType={}", request.getRecordId(), request.getVoiceType());
                } else {
                    throw new RuntimeException("summary 요청 시에는 voiceType이 필요합니다. 준비된 음성 파일이 없습니다.");
                }
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("오디오 조회/생성 중 오류 발생: nickname={}, recordId={}, error={}", 
                    request.getNickname(), request.getRecordId(), e.getMessage(), e);
            throw e; // 상위 계층에서 적절한 예외 처리 수행
        }
    }

    /**
     * 오디오 요청 파라미터 유효성 검증
     * 
     * @param request 검증할 요청 객체
     * @throws IllegalArgumentException 잘못된 파라미터인 경우
     */
    private void validateAudioRequest(AudioRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("오디오 요청 정보가 필요합니다");
        }
        
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 닉네임이 필요합니다");
        }
        
        if (request.getRecordId() == null) {
            throw new IllegalArgumentException("감정 기록 ID가 필요합니다");
        }
        
        if (request.getIsRequiredUserAudio() == null) {
            throw new IllegalArgumentException("오디오 타입 선택이 필요합니다");
        }
        
        // TTS 요청인 경우 음성 타입 검증 (null이어도 기본값 사용하므로 허용)
        if (request.isTtsRequest() && request.getVoiceType() != null) {
            // VoiceType enum 유효성은 enum 자체에서 보장됨
            log.debug("TTS 요청 검증 완료: voiceType={}", request.getVoiceType());
        }
    }

    /**
     * 사용자 및 감정 기록 조회 및 검증
     * 
     * @param nickname 사용자 닉네임
     * @param recordId 감정 기록 ID
     * @return 조회된 감정 기록
     * @throws RuntimeException 사용자 또는 감정 기록을 찾을 수 없는 경우
     */
    private EmotionRecord findEmotionRecordWithValidation(String nickname, Long recordId) {
        // 사용자 존재 여부 확인
        User user = userPersistencePort.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + nickname));
        
        // 감정 기록 존재 여부 확인 및 소유권 검증
        EmotionRecord emotionRecord = emotionRecordPersistencePort.findById(recordId)
                .orElseThrow(() -> new RuntimeException("감정 기록을 찾을 수 없습니다: " + recordId));
        
        // 감정 기록의 소유권 검증 (보안 관점에서 중요)
        if (!emotionRecord.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("해당 감정 기록에 접근할 권한이 없습니다: " + recordId);
        }
        
        log.debug("감정 기록 조회 완료: recordId={}, userId={}, hasText={}", 
                recordId, user.getId(), emotionRecord.getText() != null);
        
        return emotionRecord;
    }

    /**
     * 사용자 업로드 오디오 파일 처리
     * voiceType이 null이면 원본 음성 파일, 아니면 TTS 생성 파일 반환
     * 
     * @param emotionRecord 감정 기록
     * @param voiceType 음성 타입 (null이면 원본 파일, 아니면 TTS 생성)
     * @return 사용자 업로드 또는 TTS 오디오 응답
     */
    private AudioResponse handleUserUploadAudio(EmotionRecord emotionRecord, VoiceType voiceType) {
        log.debug("사용자 업로드 오디오 처리 시작: recordId={}, voiceType={}", emotionRecord.getId(), voiceType);
        
        if (voiceType == null) {
            // voiceType이 null이면 원본 음성 파일 반환
            if (emotionRecord.getAudioFilePath() == null || emotionRecord.getAudioFilePath().trim().isEmpty()) {
                throw new RuntimeException("업로드된 오디오 파일이 없습니다. 감정 기록 ID: " + emotionRecord.getId());
            }
            
            // S3 파일 존재 여부 확인 (옵션: 성능상 이유로 생략 가능)
            if (!s3FileService.fileExists(emotionRecord.getAudioFilePath())) {
                log.warn("S3에서 오디오 파일을 찾을 수 없음: recordId={}, path={}", 
                        emotionRecord.getId(), emotionRecord.getAudioFilePath());
                // 파일이 없어도 URL은 반환하여 클라이언트에서 처리하도록 함
            }
            
            return AudioResponse.fromUserUpload(
                    emotionRecord.getAudioFilePath(),
                    emotionRecord.getAudioFileName(),
                    emotionRecord.getAudioFileSize(),
                    emotionRecord.getAudioMimeType(),
                    emotionRecord.getAudioDuration()
            );
        } else {
            // voiceType이 지정되면 사용자 텍스트로 감정 기반 TTS 생성
            if (emotionRecord.getText() == null || emotionRecord.getText().trim().isEmpty()) {
                throw new RuntimeException("TTS 생성을 위한 텍스트가 없습니다. 감정 기록 ID: " + emotionRecord.getId());
            }
            
            try {
                // 감정 데이터 추출 및 음성 톤 계산 (사용자 업로드 요청 시에만 감정 적용)
                List<EmotionRecordCreateRequest.UserSelectedEmotion> emotions = extractEmotionsFromRecord(emotionRecord);
                VoiceToner voiceToner = calculateVoiceToner(emotions);
                
                // 캐시 키 생성 및 캐시 조회 (사용자 텍스트 사용)
                String cacheKey = generateTtsCacheKey(emotionRecord.getText(), voiceType, voiceToner);
                Optional<TtsCache> cachedTts = ttsCachePersistencePort.findByCacheKey(cacheKey);
                
                if (cachedTts.isPresent()) {
                    // 캐시 히트: 기존 파일 반환
                    log.info("사용자 업로드 TTS 캐시 히트: recordId={}, cacheKey={}", emotionRecord.getId(), cacheKey);
                    return AudioResponse.fromTtsCache(
                            cachedTts.get().getS3Url(),
                            cachedTts.get().getFileName(),
                            cachedTts.get().getFileSize(),
                            cachedTts.get().getMimeType(),
                            voiceType);
                } else {
                    // 캐시 미스: 새로운 TTS 생성
                    log.info("사용자 업로드 TTS 캐시 미스, 새로 생성: recordId={}, cacheKey={}", emotionRecord.getId(), cacheKey);
                    return generateAndCacheNewTts(emotionRecord, voiceType, voiceToner, cacheKey, emotionRecord.getText());
                }
                
            } catch (Exception e) {
                log.error("사용자 업로드 TTS 처리 중 오류 발생: recordId={}, voiceType={}, error={}", 
                        emotionRecord.getId(), voiceType, e.getMessage(), e);
                throw new RuntimeException("TTS 오디오 생성에 실패했습니다: " + e.getMessage(), e);
            }
        }
    }

    /**
     * TTS 오디오 파일 처리 (캐시 확인 후 생성)
     * summary와 가장 높은 감정점수를 가진 comment를 함께 읽어줍니다.
     * 
     * @param emotionRecord 감정 기록
     * @param voiceType 음성 타입
     * @return TTS 오디오 응답
     */
    private AudioResponse handleTtsAudio(EmotionRecord emotionRecord, VoiceType voiceType) {
        log.debug("TTS 오디오 처리 시작: recordId={}, voiceType={}", emotionRecord.getId(), voiceType);
        
        // 요약 텍스트 존재 여부 확인 (TTS는 항상 summary 사용)
        if (emotionRecord.getSummary() == null || emotionRecord.getSummary().trim().isEmpty()) {
            throw new RuntimeException("TTS 생성을 위한 요약 텍스트가 없습니다. 감정 기록 ID: " + emotionRecord.getId());
        }
        
        try {
            // 1. 가장 높은 감정점수를 가진 comment 조회
            String topEmotionComment = getTopEmotionComment(emotionRecord);
            
            // 2. summary와 comment를 조합하여 TTS용 텍스트 생성
            String combinedText = combineSummaryAndComment(emotionRecord.getSummary(), topEmotionComment);
            
            log.debug("TTS용 텍스트 생성 완료: recordId={}, originalSummaryLength={}, commentLength={}, combinedLength={}", 
                    emotionRecord.getId(), 
                    emotionRecord.getSummary().length(), 
                    topEmotionComment.length(), 
                    combinedText.length());
            
            // 3. TTS는 기본 톤으로 고정 (감정 조절 없음)
            VoiceToner voiceToner = VoiceToner.getDefaultTone();
            
            // 4. 캐시 키 생성 및 캐시 조회 (조합된 텍스트 사용)
            String cacheKey = generateTtsCacheKey(combinedText, voiceType, voiceToner);
            Optional<TtsCache> cachedTts = ttsCachePersistencePort.findByCacheKey(cacheKey);
            
            if (cachedTts.isPresent()) {
                // 캐시 히트: 기존 파일 반환
                log.info("TTS 캐시 히트: recordId={}, cacheKey={}", emotionRecord.getId(), cacheKey);
                return AudioResponse.fromTtsCache(
                        cachedTts.get().getS3Url(),
                        cachedTts.get().getFileName(),
                        cachedTts.get().getFileSize(),
                        cachedTts.get().getMimeType(),
                        voiceType);
            } else {
                // 캐시 미스: 새로운 TTS 생성
                log.info("TTS 캐시 미스, 새로 생성: recordId={}, cacheKey={}", emotionRecord.getId(), cacheKey);
                return generateAndCacheNewTts(emotionRecord, voiceType, voiceToner, cacheKey, combinedText);
            }
            
        } catch (Exception e) {
            log.error("TTS 오디오 처리 중 오류 발생: recordId={}, voiceType={}, error={}", 
                    emotionRecord.getId(), voiceType, e.getMessage(), e);
            throw new RuntimeException("TTS 오디오 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 감정 기록에서 감정 데이터 추출
     * 
     * 우선순위:
     * 1. UserSelectedEmotion (사용자가 직접 선택한 감정)
     * 2. EmotionScore 상위 3개 (시스템이 분석한 감정 점수)
     * 
     * @param emotionRecord 감정 기록
     * @return 추출된 감정 리스트
     */
    private List<EmotionRecordCreateRequest.UserSelectedEmotion> extractEmotionsFromRecord(EmotionRecord emotionRecord) {
        log.debug("감정 데이터 추출 시작: recordId={}", emotionRecord.getId());
        
        // 1. UserSelectedEmotion 우선 확인
        Optional<UserSelectedEmotion> userSelectedEmotion = userSelectedEmotionPersistencePort.findByRecord(emotionRecord);
        if (userSelectedEmotion.isPresent()) {
            UserSelectedEmotion selected = userSelectedEmotion.get();
            log.debug("사용자 선택 감정 사용: recordId={}, emotionType={}, percentage={}", 
                    emotionRecord.getId(), selected.getEmotionType(), selected.getPercentage());
            
            return List.of(EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                    .type(selected.getEmotionType().getDescription())
                    .percentage(selected.getPercentage())
                    .build());
        }
        
        // 2. UserSelectedEmotion이 없으면 EmotionScore에서 상위 감정들 추출
        List<EmotionScore> emotionScores = emotionScorePersistencePort.findByRecord(emotionRecord);
        if (emotionScores.isEmpty()) {
            log.warn("감정 데이터가 없습니다. 기본 감정 사용: recordId={}", emotionRecord.getId());
            // 기본 감정 반환 (평온함, 중간 강도)
            return List.of(EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                    .type(com.melog.melog.emotion.domain.EmotionType.CALMNESS.getDescription())
                    .percentage(50)
                    .build());
        }
        
        // 감정 점수 내림차순 정렬 후 상위 3개 선택
        List<EmotionRecordCreateRequest.UserSelectedEmotion> topEmotions = emotionScores.stream()
                .sorted((a, b) -> Integer.compare(b.getPercentage(), a.getPercentage()))
                .limit(MAX_EMOTION_COUNT_FOR_TTS)
                .map(score -> EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                        .type(score.getEmotionType().getDescription())
                        .percentage(score.getPercentage())
                        .build())
                .collect(Collectors.toList());
        
        log.debug("감정 점수 기반 감정 추출 완료: recordId={}, emotionCount={}", 
                emotionRecord.getId(), topEmotions.size());
        
        return topEmotions;
    }

    /**
     * 감정 데이터 기반 음성 톤 계산
     * 
     * VoiceToner.toneFromEmotions() 메서드를 활용하여
     * 감정 리스트로부터 음성 특성을 계산합니다.
     * 
     * @param emotions 감정 리스트
     * @return 계산된 음성 톤 설정
     */
    private VoiceToner calculateVoiceToner(List<EmotionRecordCreateRequest.UserSelectedEmotion> emotions) {
        log.debug("음성 톤 계산 시작: emotionCount={}", emotions.size());
        
        // 기존 VoiceToner.toneFromEmotions() 메서드 활용
        VoiceToner voiceToner = VoiceToner.toneFromEmotions(emotions);
        
        log.debug("음성 톤 계산 완료: volume={}, speed={}, pitch={}, emotion={}, strength={}", 
                voiceToner.getVolume(), voiceToner.getSpeed(), voiceToner.getPitch(), 
                voiceToner.getEmotion(), voiceToner.getEmotionStrength());
        
        return voiceToner;
    }

    /**
     * TTS 캐시 키 생성
     * 
     * 텍스트, 음성 타입, 음성 톤 설정, 포맷을 조합하여 MD5 해시값을 생성합니다.
     * 동일한 조합에 대해서는 항상 같은 키가 생성되어 캐시 효율성을 보장합니다.
     * 
     * @param text 원본 텍스트
     * @param voiceType 음성 타입
     * @param voiceToner 음성 톤 설정
     * @return MD5 해시 기반 캐시 키
     */
    private String generateTtsCacheKey(String text, VoiceType voiceType, VoiceToner voiceToner) {
        try {
            // VoiceToner를 JSON으로 직렬화하여 일관된 문자열 표현 생성
            String voiceTonerJson = objectMapper.writeValueAsString(voiceToner);
            
            // 캐시 키 구성 요소들을 조합
            String keyComponents = String.format("%s|%s|%s|%s", 
                    text.trim(), 
                    voiceType.name(), 
                    voiceTonerJson, 
                    DEFAULT_TTS_FORMAT);
            
            // MD5 해시 생성
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(keyComponents.getBytes(StandardCharsets.UTF_8));
            
            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String cacheKey = hexString.toString();
            log.debug("TTS 캐시 키 생성 완료: textLength={}, voiceType={}, cacheKey={}", 
                    text.length(), voiceType, cacheKey);
            
            return cacheKey;
            
        } catch (JsonProcessingException e) {
            log.error("VoiceToner JSON 직렬화 실패: error={}", e.getMessage(), e);
            throw new RuntimeException("캐시 키 생성에 실패했습니다: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 해시 알고리즘을 찾을 수 없음: error={}", e.getMessage(), e);
            throw new RuntimeException("캐시 키 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 새로운 TTS 생성 및 캐시 저장
     * 
     * @param emotionRecord 감정 기록
     * @param voiceType 음성 타입
     * @param voiceToner 음성 톤 설정
     * @param cacheKey 캐시 키
     * @param textForTts TTS 생성에 사용할 텍스트
     * @return 생성된 TTS 오디오 응답
     */
    @Transactional
    private AudioResponse generateAndCacheNewTts(EmotionRecord emotionRecord, VoiceType voiceType, 
                                                VoiceToner voiceToner, String cacheKey, String textForTts) {
        log.info("새로운 TTS 생성 시작: recordId={}, voiceType={}, cacheKey={}", 
                emotionRecord.getId(), voiceType, cacheKey);
        
        try {
            // 1. 감정 데이터 추출 (TTS 생성용) - voiceToner 기반으로 감정 리스트 생성
            List<EmotionRecordCreateRequest.UserSelectedEmotion> emotions = generateEmotionsFromToner(voiceToner);
            
            // 2. TTS 생성 요청
            TtsResponse ttsResponse = generateTtsFromService(textForTts, voiceType, emotions);
            
            // 3. S3에 업로드
            String s3Url = uploadTtsToS3(ttsResponse, emotionRecord.getUser().getNickname());
            
            // 4. 캐시에 저장
            TtsCache savedCache = saveTtsToCache(cacheKey, textForTts, voiceType, 
                    voiceToner, s3Url, ttsResponse);
            
            log.info("새로운 TTS 생성 및 캐시 저장 완료: recordId={}, cacheId={}, s3Url={}", 
                    emotionRecord.getId(), savedCache.getId(), s3Url);
            
            // 5. 응답 생성
            return AudioResponse.fromTtsGeneration(
                    s3Url,
                    savedCache.getFileName(),
                    savedCache.getFileSize(),
                    savedCache.getMimeType(),
                    voiceType
            );
            
        } catch (Exception e) {
            log.error("새로운 TTS 생성 중 오류 발생: recordId={}, voiceType={}, error={}", 
                    emotionRecord.getId(), voiceType, e.getMessage(), e);
            throw new RuntimeException("TTS 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * TTS 서비스를 통한 음성 생성
     * 
     * @param text 변환할 텍스트
     * @param voiceType 음성 타입
     * @param emotions 감정 리스트 (VoiceToner 계산용)
     * @return TTS 생성 결과
     */
    private TtsResponse generateTtsFromService(String text, VoiceType voiceType, List<EmotionRecordCreateRequest.UserSelectedEmotion> emotions) {
        log.debug("TTS 서비스 호출 시작: textLength={}, voiceType={}, emotionCount={}", text.length(), voiceType, emotions.size());
        
        TtsRequest ttsRequest = TtsRequest.builder()
                .text(text)
                .voiceType(voiceType)
                .emotions(emotions)
                .build();
        
        TtsResponse ttsResponse = textToSpeakUseCase.textToSpeak(ttsRequest);
        
        if (ttsResponse == null || ttsResponse.getAudioByteArr() == null) {
            throw new RuntimeException("TTS 서비스에서 유효한 응답을 받지 못했습니다");
        }
        
        log.debug("TTS 서비스 호출 완료: audioDataSize={}bytes", ttsResponse.getAudioByteArr().length);
        return ttsResponse;
    }

    /**
     * TTS 결과를 S3에 업로드
     * 
     * @param ttsResponse TTS 생성 결과
     * @param userId 사용자 ID
     * @return S3 업로드 URL
     */
    private String uploadTtsToS3(TtsResponse ttsResponse, String userId) {
        log.debug("TTS S3 업로드 시작: userId={}, audioDataSize={}bytes", userId, ttsResponse.getAudioByteArr().length);
        
        String fileExtension = "." + DEFAULT_TTS_FORMAT;
        String s3Url = s3FileService.uploadAudioFromByteArray(
                ttsResponse.getAudioByteArr(), 
                userId, 
                fileExtension, 
                DEFAULT_TTS_MIME_TYPE
        );
        
        log.debug("TTS S3 업로드 완료: s3Url={}", s3Url);
        return s3Url;
    }

    /**
     * VoiceToner로부터 감정 리스트 생성
     * 기본적으로 중립 감정을 반환하되, 필요시 VoiceToner의 설정에 따라 감정을 유추
     * 
     * @param voiceToner 음성 톤 설정
     * @return 생성된 감정 리스트
     */
    private List<EmotionRecordCreateRequest.UserSelectedEmotion> generateEmotionsFromToner(VoiceToner voiceToner) {
        log.debug("VoiceToner로부터 감정 생성: emotion={}, emotionStrength={}", 
                voiceToner.getEmotion(), voiceToner.getEmotionStrength());
        
        // 기본적으로 중립 감정 반환
        return List.of(EmotionRecordCreateRequest.UserSelectedEmotion.builder()
                .type(com.melog.melog.emotion.domain.EmotionType.CALMNESS.getDescription())
                .percentage(50)
                .build());
    }

    /**
     * TTS 결과를 캐시에 저장
     * 
     * @param cacheKey 캐시 키
     * @param originalText 원본 텍스트
     * @param voiceType 음성 타입
     * @param voiceToner 음성 톤 설정
     * @param s3Url S3 URL
     * @param ttsResponse TTS 응답
     * @return 저장된 캐시 엔트리
     */
    @Transactional
    private TtsCache saveTtsToCache(String cacheKey, String originalText, VoiceType voiceType, 
                                   VoiceToner voiceToner, String s3Url, TtsResponse ttsResponse) {
        log.debug("TTS 캐시 저장 시작: cacheKey={}", cacheKey);
        
        try {
            // VoiceToner를 JSON으로 직렬화
            String voiceTonerJson = objectMapper.writeValueAsString(voiceToner);
            
            // 파일명 생성 (캐시 키 기반)
            String fileName = String.format("tts_%s.%s", cacheKey.substring(0, 8), DEFAULT_TTS_FORMAT);
            
            TtsCache ttsCache = TtsCache.builder()
                    .cacheKey(cacheKey)
                    .originalText(originalText)
                    .voiceType(voiceType.name())
                    .voiceTonerJson(voiceTonerJson)
                    .s3Url(s3Url)
                    .fileName(fileName)
                    .fileSize((long) ttsResponse.getAudioByteArr().length)
                    .mimeType(DEFAULT_TTS_MIME_TYPE)
                    .build();
            
            TtsCache savedCache = ttsCachePersistencePort.save(ttsCache);
            
            log.debug("TTS 캐시 저장 완료: cacheId={}, cacheKey={}", savedCache.getId(), cacheKey);
            return savedCache;
            
        } catch (JsonProcessingException e) {
            log.error("VoiceToner JSON 직렬화 실패: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            throw new RuntimeException("캐시 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * EmotionRecord에서 가장 높은 감정점수를 가진 comment를 조회합니다.
     * 
     * 우선순위:
     * 1. UserSelectedEmotion이 있으면 해당 감정의 comment 조회
     * 2. 없으면 EmotionScore에서 가장 높은 점수를 가진 감정의 comment 조회
     * 
     * @param emotionRecord 감정 기록
     * @return 조회된 comment 텍스트 (없으면 빈 문자열)
     */
    private String getTopEmotionComment(EmotionRecord emotionRecord) {
        log.debug("가장 높은 감정점수의 comment 조회 시작: recordId={}", emotionRecord.getId());
        
        try {
            // 1. UserSelectedEmotion 우선 확인
            Optional<UserSelectedEmotion> userSelectedEmotion = userSelectedEmotionPersistencePort.findByRecord(emotionRecord);
            if (userSelectedEmotion.isPresent()) {
                UserSelectedEmotion selected = userSelectedEmotion.get();
                log.debug("사용자 선택 감정으로 comment 조회: recordId={}, emotionType={}, percentage={}", 
                        emotionRecord.getId(), selected.getEmotionType(), selected.getPercentage());
                
                return findCommentByEmotionAndPercentage(selected.getEmotionType(), selected.getPercentage());
            }
            
            // 2. UserSelectedEmotion이 없으면 EmotionScore에서 최고 점수 감정 확인
            List<EmotionScore> emotionScores = emotionScorePersistencePort.findByRecord(emotionRecord);
            if (emotionScores.isEmpty()) {
                log.warn("감정 데이터가 없어 comment를 조회할 수 없습니다: recordId={}", emotionRecord.getId());
                return "";
            }
            
            // 감정 점수 내림차순 정렬하여 최고 점수 감정 선택
            EmotionScore topEmotion = emotionScores.stream()
                    .max((a, b) -> Integer.compare(a.getPercentage(), b.getPercentage()))
                    .orElse(null);
            
            if (topEmotion == null) {
                log.warn("최고 감정 점수를 찾을 수 없습니다: recordId={}", emotionRecord.getId());
                return "";
            }
            
            log.debug("최고 감정 점수로 comment 조회: recordId={}, emotionType={}, percentage={}", 
                    emotionRecord.getId(), topEmotion.getEmotionType(), topEmotion.getPercentage());
            
            return findCommentByEmotionAndPercentage(topEmotion.getEmotionType(), topEmotion.getPercentage());
            
        } catch (Exception e) {
            log.error("감정 comment 조회 중 오류 발생: recordId={}, error={}", 
                    emotionRecord.getId(), e.getMessage(), e);
            return ""; // 오류 발생 시 빈 문자열 반환
        }
    }
    
    /**
     * 감정 타입과 퍼센티지를 기반으로 적절한 단계의 comment를 조회합니다.
     * 
     * @param emotionType 감정 타입
     * @param percentage 감정 강도 (0-100)
     * @return 조회된 comment 텍스트 (없으면 빈 문자열)
     */
    private String findCommentByEmotionAndPercentage(com.melog.melog.emotion.domain.EmotionType emotionType, Integer percentage) {
        log.debug("감정 타입과 퍼센티지로 comment 조회: emotionType={}, percentage={}", emotionType, percentage);
        
        try {
            // 퍼센티지를 1-5 단계로 변환 (20% 단위)
            Integer step = convertPercentageToStep(percentage);
            
            // 해당 감정 타입과 단계의 comment 조회
            Optional<EmotionComment> comment = emotionCommentPersistencePort.findByEmotionTypeAndStep(emotionType, step);
            
            if (comment.isPresent() && comment.get().getIsActive()) {
                log.debug("comment 조회 성공: emotionType={}, step={}, commentId={}", 
                        emotionType, step, comment.get().getId());
                return comment.get().getComment();
            } else {
                log.debug("해당 감정 타입과 단계의 활성화된 comment가 없습니다: emotionType={}, step={}", emotionType, step);
                return "";
            }
            
        } catch (Exception e) {
            log.error("감정 comment 조회 중 오류 발생: emotionType={}, percentage={}, error={}", 
                    emotionType, percentage, e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * 퍼센티지를 1-5 단계로 변환합니다.
     * 
     * @param percentage 감정 강도 (0-100)
     * @return 변환된 단계 (1-5)
     */
    private Integer convertPercentageToStep(Integer percentage) {
        if (percentage == null || percentage < 0) {
            return 1;
        }
        if (percentage <= 20) {
            return 1;
        } else if (percentage <= 40) {
            return 2;
        } else if (percentage <= 60) {
            return 3;
        } else if (percentage <= 80) {
            return 4;
        } else {
            return 5;
        }
    }
    
    /**
     * summary와 comment를 조합하여 TTS용 텍스트를 생성합니다.
     * 
     * @param summary 요약 텍스트
     * @param comment 감정 comment
     * @return 조합된 텍스트
     */
    private String combineSummaryAndComment(String summary, String comment) {
        log.debug("summary와 comment 조합 시작: summaryLength={}, commentLength={}", 
                summary != null ? summary.length() : 0, 
                comment != null ? comment.length() : 0);
        
        // summary가 없으면 comment만 반환
        if (summary == null || summary.trim().isEmpty()) {
            return comment != null && !comment.trim().isEmpty() ? comment.trim() : "";
        }
        
        // comment가 없으면 summary만 반환
        if (comment == null || comment.trim().isEmpty()) {
            return summary.trim();
        }
        
        // 둘 다 있으면 조합하여 반환
        String combinedText = summary.trim() + ". " + comment.trim();
        
        log.debug("summary와 comment 조합 완료: combinedLength={}", combinedText.length());
        return combinedText;
    }
}