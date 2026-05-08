package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysButtonPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SysButtonPermissionRepositoryJpa extends JpaRepository<SysButtonPermissionEntity, UUID> {

    List<SysButtonPermissionEntity> findByTenantIdAndMenuId(UUID tenantId, UUID menuId);

    Optional<SysButtonPermissionEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<SysButtonPermissionEntity> findByTenantId(UUID tenantId);
}
