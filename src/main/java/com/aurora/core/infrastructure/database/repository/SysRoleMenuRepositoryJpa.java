package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.SysRoleMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SysRoleMenuRepositoryJpa extends JpaRepository<SysRoleMenuEntity, UUID> {

    List<SysRoleMenuEntity> findByRoleId(UUID roleId);

    List<SysRoleMenuEntity> findByMenuId(UUID menuId);

    List<SysRoleMenuEntity> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    @Modifying
    @Query("DELETE FROM SysRoleMenuEntity e WHERE e.roleId = :roleId")
    int deleteByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Query("DELETE FROM SysRoleMenuEntity e WHERE e.roleId = :roleId AND e.menuId = :menuId")
    int deleteByRoleIdAndMenuId(@Param("roleId") UUID roleId, @Param("menuId") UUID menuId);
}
