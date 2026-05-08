package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysUserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SysUserRoleRepositoryJpa extends JpaRepository<SysUserRoleEntity, UUID> {

    List<SysUserRoleEntity> findByUserId(UUID userId);

    List<SysUserRoleEntity> findByRoleId(UUID roleId);

    List<SysUserRoleEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Modifying
    @Query("DELETE FROM SysUserRoleEntity e WHERE e.userId = :userId AND e.roleId = :roleId")
    int deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Modifying
    @Query("DELETE FROM SysUserRoleEntity e WHERE e.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
