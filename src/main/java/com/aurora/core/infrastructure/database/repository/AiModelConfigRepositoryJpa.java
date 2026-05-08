package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.AiModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiModelConfigRepositoryJpa extends JpaRepository<AiModelConfigEntity, UUID> {

    List<AiModelConfigEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<AiModelConfigEntity> findByTenantIdAndIsDefaultTrue(UUID tenantId);

    Optional<AiModelConfigEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndIsDefaultTrue(UUID tenantId);

    @Modifying
    @Query("UPDATE AiModelConfigEntity e SET e.isDefault = false WHERE e.tenantId = :tenantId")
    int clearDefaultByTenantId(@Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE AiModelConfigEntity e SET e.status = :status WHERE e.tenantId = :tenantId AND e.id = :id")
    int updateStatusByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("status") String status);

    List<AiModelConfigEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}
