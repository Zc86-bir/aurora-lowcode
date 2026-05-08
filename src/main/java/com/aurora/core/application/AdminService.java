package com.aurora.core.application;

import com.aurora.core.infrastructure.database.entity.*;
import com.aurora.core.infrastructure.database.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service — manages RBAC entities: users, roles, menus, button permissions, data rules.
 *
 * <p>Operations:
 * <ul>
 *   <li>User CRUD + role assignment</li>
 *   <li>Role CRUD + menu assignment</li>
 *   <li>Menu CRUD (tree structure)</li>
 *   <li>Button permission CRUD</li>
 *   <li>Data rule CRUD</li>
 * </ul>
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final SysUserRepositoryJpa userRepository;
    private final SysRoleRepositoryJpa roleRepository;
    private final SysMenuRepositoryJpa menuRepository;
    private final SysButtonPermissionRepositoryJpa buttonRepository;
    private final SysDataRuleRepositoryJpa dataRuleRepository;
    private final SysUserRoleRepositoryJpa userRoleRepository;
    private final SysRoleMenuRepositoryJpa roleMenuRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(SysUserRepositoryJpa userRepository,
                         SysRoleRepositoryJpa roleRepository,
                         SysMenuRepositoryJpa menuRepository,
                         SysButtonPermissionRepositoryJpa buttonRepository,
                         SysDataRuleRepositoryJpa dataRuleRepository,
                         SysUserRoleRepositoryJpa userRoleRepository,
                         SysRoleMenuRepositoryJpa roleMenuRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.buttonRepository = buttonRepository;
        this.dataRuleRepository = dataRuleRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Users ───

    public List<SysUserEntity> listUsers(UUID tenantId) {
        return userRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public Optional<SysUserEntity> getUser(UUID tenantId, UUID id) {
        return userRepository.findByTenantIdAndId(tenantId, id);
    }

    public List<String> getUserRoles(UUID tenantId, UUID userId) {
        return userRoleRepository.findByTenantIdAndUserId(tenantId, userId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SysRoleEntity::getRoleCode)
                .toList();
    }

    @Transactional
    public SysUserEntity createUser(UUID tenantId, String createdBy, String username,
                                     String email, String phone, String password) {
        SysUserEntity entity = new SysUserEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setPasswordHash(password != null ? passwordEncoder.encode(password) : null);
        entity.setCreatedBy(createdBy);
        return userRepository.save(entity);
    }

    @Transactional
    public Optional<SysUserEntity> updateUser(UUID tenantId, UUID id, String updatedBy,
                                               String username, String email, String phone, String status) {
        return userRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (username != null) entity.setUsername(username);
            if (email != null) entity.setEmail(email);
            if (phone != null) entity.setPhone(phone);
            if (status != null) entity.setStatus(status);
            entity.setUpdatedBy(updatedBy);
            return userRepository.save(entity);
        });
    }

    @Transactional
    public boolean deleteUser(UUID tenantId, UUID id) {
        return userRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            userRoleRepository.deleteByUserId(entity.getId());
            userRepository.delete(entity);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean assignRolesToUser(UUID tenantId, UUID userId, List<UUID> roleIds) {
        if (!userRepository.existsById(userId)) return false;
        userRoleRepository.deleteByUserId(userId);
        for (UUID roleId : roleIds) {
            SysUserRoleEntity link = new SysUserRoleEntity();
            link.setId(UUID.randomUUID());
            link.setUserId(userId);
            link.setRoleId(roleId);
            link.setTenantId(tenantId);
            userRoleRepository.save(link);
        }
        return true;
    }

    // ─── Roles ───

    public List<SysRoleEntity> listRoles(UUID tenantId) {
        return roleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public Optional<SysRoleEntity> getRole(UUID tenantId, UUID id) {
        return roleRepository.findByTenantIdAndId(tenantId, id);
    }

    public List<UUID> getRoleMenus(UUID tenantId, UUID roleId) {
        return roleMenuRepository.findByTenantIdAndRoleId(tenantId, roleId).stream()
                .map(SysRoleMenuEntity::getMenuId)
                .toList();
    }

    @Transactional
    public SysRoleEntity createRole(UUID tenantId, String createdBy, String roleCode,
                                     String roleName, String description) {
        SysRoleEntity entity = new SysRoleEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setRoleCode(roleCode);
        entity.setRoleName(roleName);
        entity.setDescription(description);
        entity.setCreatedBy(createdBy);
        return roleRepository.save(entity);
    }

    @Transactional
    public Optional<SysRoleEntity> updateRole(UUID tenantId, UUID id, String updatedBy,
                                               String roleCode, String roleName, String description, String status) {
        return roleRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (roleCode != null) entity.setRoleCode(roleCode);
            if (roleName != null) entity.setRoleName(roleName);
            if (description != null) entity.setDescription(description);
            if (status != null) entity.setStatus(status);
            entity.setUpdatedBy(updatedBy);
            return roleRepository.save(entity);
        });
    }

    @Transactional
    public boolean deleteRole(UUID tenantId, UUID id) {
        return roleRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            roleMenuRepository.deleteByRoleId(entity.getId());
            roleRepository.delete(entity);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean assignMenusToRole(UUID tenantId, UUID roleId, List<UUID> menuIds) {
        if (!roleRepository.existsById(roleId)) return false;
        roleMenuRepository.deleteByRoleId(roleId);
        for (UUID menuId : menuIds) {
            SysRoleMenuEntity link = new SysRoleMenuEntity();
            link.setId(UUID.randomUUID());
            link.setRoleId(roleId);
            link.setMenuId(menuId);
            link.setTenantId(tenantId);
            roleMenuRepository.save(link);
        }
        return true;
    }

    // ─── Menus ───

    public List<SysMenuEntity> listMenus(UUID tenantId) {
        return menuRepository.findByTenantIdOrderBySortOrderAsc(tenantId);
    }

    public Optional<SysMenuEntity> getMenu(UUID tenantId, UUID id) {
        return menuRepository.findByTenantIdAndId(tenantId, id);
    }

    @Transactional
    public SysMenuEntity createMenu(UUID tenantId, String createdBy, String name, String path,
                                     UUID parentId, Integer sortOrder, String icon, String type,
                                     String permissionKey) {
        SysMenuEntity entity = new SysMenuEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setName(name);
        entity.setPath(path);
        entity.setParentId(parentId);
        entity.setSortOrder(sortOrder);
        entity.setIcon(icon);
        entity.setType(type != null ? type : "MENU");
        entity.setPermissionKey(permissionKey);
        entity.setCreatedBy(createdBy);
        return menuRepository.save(entity);
    }

    @Transactional
    public Optional<SysMenuEntity> updateMenu(UUID tenantId, UUID id, String updatedBy,
                                               String name, String path, UUID parentId,
                                               Integer sortOrder, String icon, String type,
                                               String permissionKey, String status) {
        return menuRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (name != null) entity.setName(name);
            if (path != null) entity.setPath(path);
            if (parentId != null) entity.setParentId(parentId);
            if (sortOrder != null) entity.setSortOrder(sortOrder);
            if (icon != null) entity.setIcon(icon);
            if (type != null) entity.setType(type);
            if (permissionKey != null) entity.setPermissionKey(permissionKey);
            if (status != null) entity.setStatus(status);
            entity.setUpdatedBy(updatedBy);
            return menuRepository.save(entity);
        });
    }

    @Transactional
    public boolean deleteMenu(UUID tenantId, UUID id) {
        return menuRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            menuRepository.delete(entity);
            return true;
        }).orElse(false);
    }

    // ─── Button Permissions ───

    public List<SysButtonPermissionEntity> listButtons(UUID tenantId) {
        return buttonRepository.findByTenantId(tenantId);
    }

    public List<SysButtonPermissionEntity> getButtonsByMenu(UUID tenantId, UUID menuId) {
        return buttonRepository.findByTenantIdAndMenuId(tenantId, menuId);
    }

    public Optional<SysButtonPermissionEntity> getButton(UUID tenantId, UUID id) {
        return buttonRepository.findByTenantIdAndId(tenantId, id);
    }

    @Transactional
    public SysButtonPermissionEntity createButton(UUID tenantId, String createdBy, UUID menuId,
                                                    String buttonCode, String buttonName, String permissionKey) {
        SysButtonPermissionEntity entity = new SysButtonPermissionEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setMenuId(menuId);
        entity.setButtonCode(buttonCode);
        entity.setButtonName(buttonName);
        entity.setPermissionKey(permissionKey);
        entity.setCreatedBy(createdBy);
        return buttonRepository.save(entity);
    }

    @Transactional
    public Optional<SysButtonPermissionEntity> updateButton(UUID tenantId, UUID id, String updatedBy,
                                                              String buttonCode, String buttonName,
                                                              String permissionKey, String status) {
        return buttonRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (buttonCode != null) entity.setButtonCode(buttonCode);
            if (buttonName != null) entity.setButtonName(buttonName);
            if (permissionKey != null) entity.setPermissionKey(permissionKey);
            if (status != null) entity.setStatus(status);
            return buttonRepository.save(entity);
        });
    }

    @Transactional
    public boolean deleteButton(UUID tenantId, UUID id) {
        return buttonRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            buttonRepository.delete(entity);
            return true;
        }).orElse(false);
    }

    // ─── Data Rules ───

    public List<SysDataRuleEntity> listDataRules(UUID tenantId) {
        return dataRuleRepository.findByTenantId(tenantId);
    }

    public List<SysDataRuleEntity> getDataRulesByRole(UUID tenantId, UUID roleId) {
        return dataRuleRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    public Optional<SysDataRuleEntity> getDataRule(UUID tenantId, UUID id) {
        return dataRuleRepository.findByTenantIdAndId(tenantId, id);
    }

    @Transactional
    public SysDataRuleEntity createDataRule(UUID tenantId, String createdBy, UUID roleId,
                                              String resourceType, String ruleName,
                                              Map<String, Object> ruleExpression) {
        SysDataRuleEntity entity = new SysDataRuleEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setRoleId(roleId);
        entity.setResourceType(resourceType);
        entity.setRuleName(ruleName);
        entity.setRuleExpression(ruleExpression);
        entity.setCreatedBy(createdBy);
        return dataRuleRepository.save(entity);
    }

    @Transactional
    public Optional<SysDataRuleEntity> updateDataRule(UUID tenantId, UUID id, String updatedBy,
                                                        String ruleName, Map<String, Object> ruleExpression,
                                                        String status) {
        return dataRuleRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (ruleName != null) entity.setRuleName(ruleName);
            if (ruleExpression != null) entity.setRuleExpression(ruleExpression);
            if (status != null) entity.setStatus(status);
            entity.setUpdatedBy(updatedBy);
            return dataRuleRepository.save(entity);
        });
    }

    @Transactional
    public boolean deleteDataRule(UUID tenantId, UUID id) {
        return dataRuleRepository.findByTenantIdAndId(tenantId, id).map(entity -> {
            dataRuleRepository.delete(entity);
            return true;
        }).orElse(false);
    }

    // ─── Response helpers ───

    public Map<String, Object> toUserResponse(SysUserEntity entity) {
        return Map.of(
                "id", entity.getId().toString(),
                "username", entity.getUsername(),
                "email", entity.getEmail() != null ? entity.getEmail() : "",
                "phone", entity.getPhone() != null ? entity.getPhone() : "",
                "status", entity.getStatus(),
                "roles", getUserRoles(entity.getTenantId(), entity.getId()),
                "createdAt", entity.getCreatedAt().toString(),
                "updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : ""
        );
    }

    public Map<String, Object> toRoleResponse(SysRoleEntity entity) {
        return Map.of(
                "id", entity.getId().toString(),
                "roleCode", entity.getRoleCode(),
                "roleName", entity.getRoleName(),
                "description", entity.getDescription() != null ? entity.getDescription() : "",
                "status", entity.getStatus(),
                "createdAt", entity.getCreatedAt().toString(),
                "updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : ""
        );
    }

    public Map<String, Object> toMenuResponse(SysMenuEntity entity) {
        return Map.of(
                "id", entity.getId().toString(),
                "name", entity.getName(),
                "path", entity.getPath() != null ? entity.getPath() : "",
                "parentId", entity.getParentId() != null ? entity.getParentId().toString() : "",
                "sortOrder", entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                "icon", entity.getIcon() != null ? entity.getIcon() : "",
                "type", entity.getType(),
                "permissionKey", entity.getPermissionKey() != null ? entity.getPermissionKey() : "",
                "status", entity.getStatus(),
                "createdAt", entity.getCreatedAt().toString()
        );
    }

    public Map<String, Object> toButtonResponse(SysButtonPermissionEntity entity) {
        return Map.of(
                "id", entity.getId().toString(),
                "menuId", entity.getMenuId().toString(),
                "buttonCode", entity.getButtonCode(),
                "buttonName", entity.getButtonName(),
                "permissionKey", entity.getPermissionKey(),
                "status", entity.getStatus(),
                "createdAt", entity.getCreatedAt().toString()
        );
    }

    public Map<String, Object> toDataRuleResponse(SysDataRuleEntity entity) {
        return Map.of(
                "id", entity.getId().toString(),
                "roleId", entity.getRoleId().toString(),
                "resourceType", entity.getResourceType(),
                "ruleName", entity.getRuleName(),
                "ruleExpression", entity.getRuleExpression() != null ? entity.getRuleExpression() : Map.of(),
                "status", entity.getStatus(),
                "createdAt", entity.getCreatedAt().toString(),
                "updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : ""
        );
    }
}
