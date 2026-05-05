package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.MetadataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link MetadataEntity}.
 *
 * <p>All queries are tenant-scoped. The Hibernate tenant filter
 * ({@code @FilterDef}) ensures row-level security by injecting
 * {@code WHERE tenant_id = :tenantId} automatically.
 */
@Repository
public interface MetadataRepositoryJpa extends JpaRepository<MetadataEntity, UUID> {

    List<MetadataEntity> findByTenantId(UUID tenantId);

    List<MetadataEntity> findByType(String type);

    Optional<MetadataEntity> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    List<MetadataEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT m FROM MetadataEntity m WHERE m.updatedAt > :timestamp")
    List<MetadataEntity> findModifiedAfter(@Param("timestamp") Instant timestamp);

    @Query("SELECT m FROM MetadataEntity m WHERE m.tenantId = :tenantId AND m.version = :version")
    Optional<MetadataEntity> findByTenantIdAndVersion(
            @Param("tenantId") UUID tenantId,
            @Param("version") int version);

    @Modifying
    @Query("UPDATE MetadataEntity m SET m.status = :status WHERE m.id IN :ids")
    void updateStatusByIds(@Param("ids") List<UUID> ids,
                           @Param("status") String status);

    @Query("SELECT COUNT(m) FROM MetadataEntity m WHERE m.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    Page<MetadataEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT m FROM MetadataEntity m ORDER BY m.updatedAt DESC")
    Page<MetadataEntity> findAllPaged(Pageable pageable);
}
