package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.AuditChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link AuditChainEntity}.
 *
 * <p>Append-only audit log. Entities are immutable after creation.
 */
@Repository
public interface AuditChainJpa extends JpaRepository<AuditChainEntity, UUID> {

    List<AuditChainEntity> findByTenantIdOrderBySeqNumAsc(UUID tenantId);

    @Query("SELECT a FROM AuditChainEntity a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :from AND :to ORDER BY a.seqNum ASC")
    List<AuditChainEntity> findByTenantIdAndCreatedAtBetween(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT MAX(a.seqNum) FROM AuditChainEntity a WHERE a.tenantId = :tenantId")
    Long findMaxSeqNumByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM AuditChainEntity a WHERE a.tenantId = :tenantId AND a.seqNum = :seqNum")
    AuditChainEntity findByTenantIdAndSeqNum(
            @Param("tenantId") UUID tenantId,
            @Param("seqNum") long seqNum);

    @Query("SELECT COUNT(a) FROM AuditChainEntity a WHERE a.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
