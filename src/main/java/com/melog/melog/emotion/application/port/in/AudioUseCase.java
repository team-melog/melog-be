package com.melog.melog.emotion.application.port.in;

import com.melog.melog.emotion.domain.model.request.AudioRequest;
import com.melog.melog.emotion.domain.model.response.AudioResponse;

/**
 * 오디오 처리 유스케이스
 * 
 * 오디오 파일 조회 및 생성에 관한 비즈니스 로직을 정의하는 인바운드 포트입니다.
 * 헥사고날 아키텍처에서 애플리케이션 계층의 진입점 역할을 하며,
 * 클라이언트의 오디오 요청을 처리합니다.
 * 
 * 주요 기능:
 * - 사용자 업로드 오디오 파일 조회
 * - TTS(Text-To-Speech) 오디오 생성 및 조회
 * - TTS 캐시 관리를 통한 성능 최적화
 * 
 * 비즈니스 규칙:
 * - isRequiredUserAudio가 true인 경우: 기존 업로드된 음성 파일 반환
 * - isRequiredUserAudio가 false인 경우: TTS로 생성하거나 캐시된 파일 반환
 * - 감정 기록이 존재하지 않는 경우 예외 발생
 * - TTS 생성 실패 시 적절한 예외 처리
 * 
 * @author Melog Team
 * @since 1.0
 */
public interface AudioUseCase {

    /**
     * 오디오 파일 조회 또는 생성
     * 
     * 요청 타입에 따라 사용자 업로드 파일을 반환하거나 TTS로 새로운 오디오를 생성합니다.
     * TTS 요청의 경우 기존 캐시를 우선 확인하고, 없을 경우에만 새로 생성합니다.
     * 
     * 처리 흐름:
     * 1. 사용자 및 감정 기록 존재 여부 확인
     * 2. 요청 타입에 따른 분기 처리
     *    - 사용자 업로드: 기존 파일 정보 반환
     *    - TTS 생성: 캐시 확인 → 없으면 새로 생성
     * 3. 감정 데이터 추출 및 음성 톤 계산 (TTS인 경우)
     * 4. 파일 정보 응답 구성
     * 
     * 성능 고려사항:
     * - TTS 캐시를 통한 중복 생성 방지
     * - 감정 데이터 조회 최적화
     * - S3 URL 생성 시간 최소화
     * 
     * @param request 오디오 요청 정보 (사용자, 기록ID, 타입, 음성설정 포함)
     * @return 오디오 파일 정보 및 메타데이터
     * @throws IllegalArgumentException 잘못된 요청 파라미터인 경우
     * @throws RuntimeException 사용자 또는 감정 기록을 찾을 수 없는 경우
     * @throws RuntimeException TTS 생성 중 오류가 발생한 경우
     * @throws RuntimeException S3 파일 처리 중 오류가 발생한 경우
     */
    AudioResponse getOrCreateAudio(AudioRequest request);
}