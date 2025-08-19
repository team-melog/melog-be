package com.melog.melog.emotion.application.port.out;

import com.melog.melog.emotion.domain.TtsCache;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TTS 캐시 영속성 포트
 * 
 * TTS 캐시 데이터의 저장, 조회, 관리를 위한 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 도메인 계층이 인프라스트럭처 계층의 
 * 데이터베이스 접근 기능을 사용하기 위한 인터페이스입니다.
 * 
 * 주요 기능:
 * - 캐시 키 기반 캐시 조회
 * - 새로운 캐시 엔트리 저장
 * - 캐시 접근 시간 업데이트
 * - 만료된 캐시 정리
 * 
 * @author Melog Team
 * @since 1.0
 */
public interface TtsCachePersistencePort {

    /**
     * 캐시 키로 TTS 캐시 조회
     * 
     * 캐시 히트 시 lastAccessedAt 필드가 자동으로 업데이트됩니다.
     * 이는 캐시 만료 정책 수립에 활용됩니다.
     * 
     * @param cacheKey 조회할 캐시 키 (MD5 해시값)
     * @return 캐시 엔트리 (존재하지 않으면 Optional.empty())
     */
    Optional<TtsCache> findByCacheKey(String cacheKey);

    /**
     * TTS 캐시 저장
     * 
     * 새로운 TTS 생성 결과를 캐시에 저장합니다.
     * 중복된 캐시 키가 존재하는 경우 예외가 발생할 수 있습니다.
     * 
     * @param ttsCache 저장할 TTS 캐시 엔트리
     * @return 저장된 TTS 캐시 엔트리 (ID 포함)
     */
    TtsCache save(TtsCache ttsCache);

    /**
     * 캐시 접근 시간 업데이트
     * 
     * 캐시 히트 시 마지막 접근 시간을 현재 시간으로 업데이트합니다.
     * 캐시 만료 정책에서 최근 사용 여부를 판단하는 데 사용됩니다.
     * 
     * @param cacheKey 업데이트할 캐시 키
     * @param accessTime 접근 시간
     */
    void updateLastAccessedTime(String cacheKey, LocalDateTime accessTime);

    /**
     * 지정된 기간 이상 사용되지 않은 캐시 엔트리 조회
     * 
     * 캐시 정리 작업에서 삭제 대상을 식별하는 데 사용됩니다.
     * 배치 작업이나 관리 도구에서 호출됩니다.
     * 
     * @param days 기준 일수
     * @return 만료된 캐시 엔트리 목록
     */
    List<TtsCache> findNotAccessedForDays(int days);

    /**
     * 캐시 엔트리 삭제
     * 
     * 만료된 캐시나 불필요한 캐시를 삭제합니다.
     * S3 파일도 함께 삭제해야 하므로 신중하게 사용해야 합니다.
     * 
     * @param cacheKey 삭제할 캐시 키
     */
    void deleteByCacheKey(String cacheKey);

    /**
     * 여러 캐시 엔트리 일괄 삭제
     * 
     * 배치 삭제 작업에서 성능 향상을 위해 사용됩니다.
     * 
     * @param cacheKeys 삭제할 캐시 키 목록
     */
    void deleteByCacheKeys(List<String> cacheKeys);

    /**
     * 전체 캐시 개수 조회
     * 
     * 모니터링이나 관리 목적으로 현재 캐시된 항목의 개수를 반환합니다.
     * 
     * @return 전체 캐시 엔트리 개수
     */
    long countAll();

    /**
     * 특정 기간 내 생성된 캐시 개수 조회
     * 
     * TTS 사용량 모니터링에 활용됩니다.
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 해당 기간 내 생성된 캐시 개수
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 캐시 히트율 계산을 위한 통계 정보 조회
     * 
     * 캐시 효율성 모니터링에 활용됩니다.
     * 실제 구현에서는 별도의 통계 테이블이나 로그 분석이 필요할 수 있습니다.
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 캐시 접근 횟수
     */
    long countAccessesBetween(LocalDateTime startDate, LocalDateTime endDate);
}