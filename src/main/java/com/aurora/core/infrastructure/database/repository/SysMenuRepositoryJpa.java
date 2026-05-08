package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SysMenuRepositoryJpa extends JpaRepository<SysMenuEntity, UUID> {

    List<SysMenuEntity> findByTenantIdOrderBySortOrderAsc(UUID tenantId);

    Optional<SysMenuEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<SysMenuEntity> findByTenantIdAndParentIdOrderBySortOrderAsc(UUID tenantId, UUID parentId);

    List<SysMenuEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}
