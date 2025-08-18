package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.application.port.out.TtsCachePersistencePort;
import com.melog.melog.emotion.domain.TtsCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TTS 캐시 영속성 어댑터
 * 
 * TtsCachePersistencePort 인터페이스를 구현하여 실제 데이터베이스 접근을 담당하는 어댑터입니다.
 * 헥사고날 아키텍처에서 인프라스트럭처 계층에 해당하며,
 * 도메인 계층의 포트를 JPA 레포지토리와 연결하는 역할을 합니다.
 * 
 * 주요 책임:
 * - 도메인 포트와 JPA 레포지토리 간의 어댑팅
 * - 트랜잭션 관리
 * - 데이터베이스 예외 처리 및 로깅
 * - 캐시 접근 시간 자동 업데이트
 * 
 * @author Melog Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TtsCachePersistenceAdapter implements TtsCachePersistencePort {

    private final TtsCacheJpaRepository ttsCacheJpaRepository;

    /**
     * 캐시 키로 TTS 캐시 조회 및 접근 시간 업데이트
     * 
     * 캐시 히트 시 자동으로 lastAccessedAt 필드를 현재 시간으로 업데이트합니다.
     * 이는 캐시 만료 정책 수립에 중요한 정보를 제공합니다.
     * 
     * 성능 고려사항:
     * - 조회와 업데이트가 별도 트랜잭션으로 분리되어 조회 성능에 영향 최소화
     * - 업데이트 실패 시에도 조회 결과는 정상적으로 반환
     * 
     * @param cacheKey 조회할 캐시 키 (MD5 해시값)
     * @return 캐시 엔트리 (존재하지 않으면 Optional.empty())
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<TtsCache> findByCacheKey(String cacheKey) {
        log.debug("TTS 캐시 조회 시작: cacheKey={}", cacheKey);
        
        try {
            Optional<TtsCache> cacheEntry = ttsCacheJpaRepository.findByCacheKey(cacheKey);
            
            if (cacheEntry.isPresent()) {
                log.debug("TTS 캐시 히트: cacheKey={}, fileName={}", cacheKey, cacheEntry.get().getFileName());
                
                // 캐시 히트 시 접근 시간 업데이트 (별도 트랜잭션)
                updateLastAccessedTimeAsync(cacheKey);
                
                return cacheEntry;
            } else {
                log.debug("TTS 캐시 미스: cacheKey={}", cacheKey);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("TTS 캐시 조회 중 오류 발생: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            // 조회 실패 시에도 빈 Optional 반환하여 비즈니스 로직 계속 진행
            return Optional.empty();
        }
    }

    /**
     * TTS 캐시 저장
     * 
     * 새로운 TTS 생성 결과를 캐시에 저장합니다.
     * 캐시 키 중복 시 예외가 발생하며, 이는 동시성 제어 관점에서 정상적인 동작입니다.
     * 
     * @param ttsCache 저장할 TTS 캐시 엔트리
     * @return 저장된 TTS 캐시 엔트리 (ID 포함)
     * @throws RuntimeException 저장 실패 시
     */
    @Override
    @Transactional
    public TtsCache save(TtsCache ttsCache) {
        log.debug("TTS 캐시 저장 시작: cacheKey={}, fileName={}", 
                ttsCache.getCacheKey(), ttsCache.getFileName());
        
        try {
            TtsCache savedCache = ttsCacheJpaRepository.save(ttsCache);
            
            log.info("TTS 캐시 저장 완료: id={}, cacheKey={}, fileName={}, fileSize={}bytes", 
                    savedCache.getId(), savedCache.getCacheKey(), 
                    savedCache.getFileName(), savedCache.getFileSize());
            
            return savedCache;
        } catch (Exception e) {
            log.error("TTS 캐시 저장 중 오류 발생: cacheKey={}, error={}", 
                    ttsCache.getCacheKey(), e.getMessage(), e);
            throw new RuntimeException("TTS 캐시 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 캐시 접근 시간 업데이트
     * 
     * 캐시 히트 시 마지막 접근 시간을 현재 시간으로 업데이트합니다.
     * 별도 트랜잭션으로 처리하여 메인 로직에 영향을 주지 않습니다.
     * 
     * @param cacheKey 업데이트할 캐시 키
     * @param accessTime 접근 시간
     */
    @Override
    @Transactional
    public void updateLastAccessedTime(String cacheKey, LocalDateTime accessTime) {
        log.debug("캐시 접근 시간 업데이트: cacheKey={}, accessTime={}", cacheKey, accessTime);
        
        try {
            int updatedCount = ttsCacheJpaRepository.updateLastAccessedTime(cacheKey, accessTime);
            
            if (updatedCount > 0) {
                log.debug("캐시 접근 시간 업데이트 완료: cacheKey={}", cacheKey);
            } else {
                log.warn("캐시 접근 시간 업데이트 실패: 대상을 찾을 수 없음, cacheKey={}", cacheKey);
            }
        } catch (Exception e) {
            log.error("캐시 접근 시간 업데이트 중 오류 발생: cacheKey={}, error={}", 
                    cacheKey, e.getMessage(), e);
            // 접근 시간 업데이트 실패는 비즈니스 로직에 영향을 주지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * 지정된 기간 이상 사용되지 않은 캐시 엔트리 조회
     * 
     * 캐시 정리 작업에서 삭제 대상을 식별하는 데 사용됩니다.
     * 배치 작업이나 관리 도구에서 호출됩니다.
     * 
     * @param days 기준 일수
     * @return 만료된 캐시 엔트리 목록
     */
    @Override
    public List<TtsCache> findNotAccessedForDays(int days) {
        log.debug("만료 캐시 조회 시작: days={}", days);
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            List<TtsCache> expiredCaches = ttsCacheJpaRepository.findByLastAccessedAtBefore(cutoffDate);
            
            log.info("만료 캐시 조회 완료: days={}, count={}", days, expiredCaches.size());
            return expiredCaches;
        } catch (Exception e) {
            log.error("만료 캐시 조회 중 오류 발생: days={}, error={}", days, e.getMessage(), e);
            throw new RuntimeException("만료 캐시 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 캐시 엔트리 삭제
     * 
     * 만료된 캐시나 불필요한 캐시를 삭제합니다.
     * 주의: S3 파일도 함께 삭제해야 하므로 신중하게 사용해야 합니다.
     * 
     * @param cacheKey 삭제할 캐시 키
     */
    @Override
    @Transactional
    public void deleteByCacheKey(String cacheKey) {
        log.debug("캐시 삭제 시작: cacheKey={}", cacheKey);
        
        try {
            ttsCacheJpaRepository.deleteByCacheKey(cacheKey);
            log.info("캐시 삭제 완료: cacheKey={}", cacheKey);
        } catch (Exception e) {
            log.error("캐시 삭제 중 오류 발생: cacheKey={}, error={}", cacheKey, e.getMessage(), e);
            throw new RuntimeException("캐시 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여러 캐시 엔트리 일괄 삭제
     * 
     * 배치 삭제 작업에서 성능 향상을 위해 사용됩니다.
     * 대량 삭제 시 메모리 사용량에 주의해야 합니다.
     * 
     * @param cacheKeys 삭제할 캐시 키 목록
     */
    @Override
    @Transactional
    public void deleteByCacheKeys(List<String> cacheKeys) {
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            log.debug("삭제할 캐시 키가 없습니다");
            return;
        }
        
        log.debug("캐시 일괄 삭제 시작: count={}", cacheKeys.size());
        
        try {
            // 대량 삭제 시 청크 단위로 분할 처리 (메모리 효율성)
            int chunkSize = 100;
            for (int i = 0; i < cacheKeys.size(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, cacheKeys.size());
                List<String> chunk = cacheKeys.subList(i, endIndex);
                
                ttsCacheJpaRepository.deleteByCacheKeyIn(chunk);
                log.debug("캐시 청크 삭제 완료: {}/{}", endIndex, cacheKeys.size());
            }
            
            log.info("캐시 일괄 삭제 완료: count={}", cacheKeys.size());
        } catch (Exception e) {
            log.error("캐시 일괄 삭제 중 오류 발생: count={}, error={}", cacheKeys.size(), e.getMessage(), e);
            throw new RuntimeException("캐시 일괄 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 전체 캐시 개수 조회
     * 
     * @return 전체 캐시 엔트리 개수
     */
    @Override
    public long countAll() {
        try {
            long count = ttsCacheJpaRepository.count();
            log.debug("전체 캐시 개수 조회: count={}", count);
            return count;
        } catch (Exception e) {
            log.error("전체 캐시 개수 조회 중 오류 발생: error={}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 특정 기간 내 생성된 캐시 개수 조회
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 해당 기간 내 생성된 캐시 개수
     */
    @Override
    public long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("기간별 캐시 개수 조회: startDate={}, endDate={}", startDate, endDate);
        
        try {
            long count = ttsCacheJpaRepository.countByCreatedAtBetween(startDate, endDate);
            log.debug("기간별 캐시 개수 조회 완료: count={}", count);
            return count;
        } catch (Exception e) {
            log.error("기간별 캐시 개수 조회 중 오류 발생: startDate={}, endDate={}, error={}", 
                    startDate, endDate, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 캐시 히트율 계산을 위한 통계 정보 조회
     * 
     * 현재는 단순히 접근 시간 기준으로 카운트하지만,
     * 실제로는 별도의 접근 로그나 메트릭 수집이 필요할 수 있습니다.
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 캐시 접근 횟수 (근사치)
     */
    @Override
    public long countAccessesBetween(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("캐시 접근 통계 조회: startDate={}, endDate={}", startDate, endDate);
        
        try {
            // 현재는 단순히 해당 기간에 접근된 캐시 엔트리 수를 반환
            // 실제 접근 횟수 추적을 위해서는 별도의 로깅 또는 카운터 필요
            List<TtsCache> accessedCaches = ttsCacheJpaRepository.findByLastAccessedAtBefore(endDate);
            long count = accessedCaches.stream()
                    .filter(cache -> cache.getLastAccessedAt().isAfter(startDate))
                    .count();
            
            log.debug("캐시 접근 통계 조회 완료: count={}", count);
            return count;
        } catch (Exception e) {
            log.error("캐시 접근 통계 조회 중 오류 발생: startDate={}, endDate={}, error={}", 
                    startDate, endDate, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 캐시 접근 시간을 비동기적으로 업데이트
     * 
     * 메인 로직의 성능에 영향을 주지 않기 위해 별도 트랜잭션으로 처리합니다.
     * 업데이트 실패 시에도 메인 로직은 정상 진행됩니다.
     * 
     * @param cacheKey 업데이트할 캐시 키
     */
    private void updateLastAccessedTimeAsync(String cacheKey) {
        // 현재는 동기 호출이지만, 향후 @Async 어노테이션을 사용하여 비동기 처리 가능
        updateLastAccessedTime(cacheKey, LocalDateTime.now());
    }
}