package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SkillRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link SkillRegistryEntity}.
 */
@Repository
public interface SkillRegistryJpa extends JpaRepository<SkillRegistryEntity, UUID> {

    List<SkillRegistryEntity> findByTenantId(UUID tenantId);

    Optional<SkillRegistryEntity> findByTenantIdAndSkillId(UUID tenantId, String skillId);

    boolean existsByTenantIdAndSkillId(UUID tenantId, String skillId);

    List<SkillRegistryEntity> findByTenantIdAndCategory(UUID tenantId, String category);

    List<SkillRegistryEntity> findByTenantIdAndDeprecatedFalse(UUID tenantId);

    @Query("SELECT s FROM SkillRegistryEntity s WHERE s.tenantId = :tenantId AND s.jeecgCompat = true")
    List<SkillRegistryEntity> findJeecgCompatByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM SkillRegistryEntity s WHERE s.tenantId = :tenantId AND s.deprecated = false")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);
}
