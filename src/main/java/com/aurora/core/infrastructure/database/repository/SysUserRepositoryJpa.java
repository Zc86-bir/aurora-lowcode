package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SysUserRepositoryJpa extends JpaRepository<SysUserEntity, UUID> {

    List<SysUserEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<SysUserEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<SysUserEntity> findByTenantIdAndEmail(UUID tenantId, String email);

    List<SysUserEntity> findByTenantIdAndStatus(UUID tenantId, String status);
}
