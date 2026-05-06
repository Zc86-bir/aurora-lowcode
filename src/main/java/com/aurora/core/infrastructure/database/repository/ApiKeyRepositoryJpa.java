package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link ApiKeyEntity}.
 */
@Repository
public interface ApiKeyRepositoryJpa extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByHashedKeyAndStatus(String hashedKey, String status);

    List<ApiKeyEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT k FROM ApiKeyEntity k WHERE k.tenantId = :tenantId AND k.status = 'ACTIVE' ORDER BY k.createdAt DESC")
    List<ApiKeyEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    boolean existsByHashedKey(String hashedKey);
}
