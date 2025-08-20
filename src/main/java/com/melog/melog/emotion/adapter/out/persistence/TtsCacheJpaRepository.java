package com.melog.melog.emotion.adapter.out.persistence;

import com.melog.melog.emotion.domain.TtsCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TTS 캐시 JPA 레포지토리
 * 
 * TTS 캐시 엔티티에 대한 데이터베이스 접근을 제공하는 JPA 레포지토리입니다.
 * Spring Data JPA의 기본 CRUD 기능과 함께 캐시 특화된 쿼리 메서드를 제공합니다.
 * 
 * 성능 고려사항:
 * - 캐시 키 조회는 인덱스를 활용하여 빠른 검색 지원
 * - 배치 업데이트/삭제 작업은 @Modifying 어노테이션 사용
 * - 통계 쿼리는 적절한 인덱스 설계 전제
 * 
 * @author Melog Team
 * @since 1.0
 */
@Repository
public interface TtsCacheJpaRepository extends JpaRepository<TtsCache, Long> {

    /**
     * 캐시 키로 TTS 캐시 조회
     * 
     * 캐시 키에는 유니크 제약조건이 있어 최대 1개의 결과만 반환됩니다.
     * 인덱스를 활용하여 빠른 검색이 가능합니다.
     * 
     * @param cacheKey 조회할 캐시 키
     * @return TTS 캐시 엔트리 (Optional)
     */
    Optional<TtsCache> findByCacheKey(String cacheKey);

    /**
     * 지정된 일수 이상 접근되지 않은 캐시 엔트리 조회
     * 
     * 캐시 정리 정책에 활용되는 쿼리입니다.
     * lastAccessedAt 필드에 인덱스가 있어야 성능상 유리합니다.
     * 
     * @param cutoffDate 기준 일시 (이 시간 이전에 마지막 접근한 캐시들이 대상)
     * @return 만료 대상 캐시 엔트리 목록
     */
    @Query("SELECT tc FROM TtsCache tc WHERE tc.lastAccessedAt < :cutoffDate ORDER BY tc.lastAccessedAt ASC")
    List<TtsCache> findByLastAccessedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 마지막 접근 시간 일괄 업데이트
     * 
     * 캐시 히트 시 접근 시간을 업데이트합니다.
     * @Modifying 어노테이션으로 벌크 업데이트 수행합니다.
     * 
     * @param cacheKey 업데이트할 캐시 키
     * @param accessTime 새로운 접근 시간
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE TtsCache tc SET tc.lastAccessedAt = :accessTime WHERE tc.cacheKey = :cacheKey")
    int updateLastAccessedTime(@Param("cacheKey") String cacheKey, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 캐시 키로 캐시 엔트리 삭제
     * 
     * @param cacheKey 삭제할 캐시 키
     */
    void deleteByCacheKey(String cacheKey);

    /**
     * 여러 캐시 키로 일괄 삭제
     * 
     * 배치 삭제 작업에서 성능 향상을 위해 사용됩니다.
     * IN 절을 사용하므로 키 개수가 많을 경우 분할 처리가 필요할 수 있습니다.
     * 
     * @param cacheKeys 삭제할 캐시 키 목록
     */
    @Modifying
    @Query("DELETE FROM TtsCache tc WHERE tc.cacheKey IN :cacheKeys")
    void deleteByCacheKeyIn(@Param("cacheKeys") List<String> cacheKeys);

    /**
     * 생성 시간 범위로 캐시 개수 조회
     * 
     * TTS 사용량 모니터링에 활용됩니다.
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 해당 기간 내 생성된 캐시 개수
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 음성 타입의 캐시 개수 조회
     * 
     * 음성 타입별 사용 통계에 활용됩니다.
     * 
     * @param voiceType 조회할 음성 타입
     * @return 해당 음성 타입의 캐시 개수
     */
    long countByVoiceType(String voiceType);

    /**
     * 총 캐시 파일 크기 조회
     * 
     * 스토리지 사용량 모니터링에 활용됩니다.
     * SUM 함수 사용 시 NULL 처리에 주의가 필요합니다.
     * 
     * @return 총 파일 크기 (바이트 단위, null인 경우 0)
     */
    @Query("SELECT COALESCE(SUM(tc.fileSize), 0) FROM TtsCache tc")
    Long getTotalCacheSize();

    /**
     * 최근 생성된 캐시 엔트리 조회
     * 
     * 관리 도구나 모니터링에서 최근 활동을 확인하는 데 사용됩니다.
     * 
     * @param limit 조회할 최대 개수
     * @return 최근 생성된 캐시 엔트리 목록
     */
    @Query("SELECT tc FROM TtsCache tc ORDER BY tc.createdAt DESC LIMIT :limit")
    List<TtsCache> findRecentlyCreated(@Param("limit") int limit);

    /**
     * 자주 사용되는 캐시 엔트리 조회 (접근 시간 기준)
     * 
     * 캐시 효율성 분석에 활용됩니다.
     * 
     * @param limit 조회할 최대 개수
     * @return 최근 접근된 캐시 엔트리 목록
     */
    @Query("SELECT tc FROM TtsCache tc ORDER BY tc.lastAccessedAt DESC LIMIT :limit")
    List<TtsCache> findRecentlyAccessed(@Param("limit") int limit);

    /**
     * 특정 텍스트로 생성된 캐시 엔트리 조회
     * 
     * 동일한 텍스트에 대한 다양한 음성 타입의 캐시를 확인할 때 사용됩니다.
     * 전문 검색이 필요한 경우 별도의 검색 엔진 활용을 고려해야 합니다.
     * 
     * @param originalText 검색할 원본 텍스트
     * @return 해당 텍스트로 생성된 캐시 엔트리 목록
     */
    List<TtsCache> findByOriginalTextContaining(String originalText);
}