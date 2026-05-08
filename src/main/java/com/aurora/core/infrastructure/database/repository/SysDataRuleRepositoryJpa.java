package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysDataRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SysDataRuleRepositoryJpa extends JpaRepository<SysDataRuleEntity, UUID> {

    List<SysDataRuleEntity> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    Optional<SysDataRuleEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<SysDataRuleEntity> findByTenantIdAndResourceType(UUID tenantId, String resourceType);

    List<SysDataRuleEntity> findByTenantId(UUID tenantId);
}
