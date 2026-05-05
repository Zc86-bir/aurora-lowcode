package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link TenantEntity}.
 */
@Repository
public interface TenantRepositoryJpa extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByTenantCode(String tenantCode);

    boolean existsByTenantCode(String tenantCode);

    @Query("SELECT t FROM TenantEntity t WHERE t.status = 'ACTIVE' AND t.deletedAt IS NULL")
    java.util.List<TenantEntity> findAllActive();

    @Query("SELECT t FROM TenantEntity t WHERE t.tenantCode = :code AND t.status = 'ACTIVE'")
    Optional<TenantEntity> findActiveByCode(@Param("code") String code);
}
