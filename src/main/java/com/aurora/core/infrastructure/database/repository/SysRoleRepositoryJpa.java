package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SysRoleRepositoryJpa extends JpaRepository<SysRoleEntity, UUID> {

    List<SysRoleEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<SysRoleEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<SysRoleEntity> findByTenantIdAndRoleCode(UUID tenantId, String roleCode);

    List<SysRoleEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}
