package com.aurora.core.adapter.web;

import com.aurora.core.application.AdminService;
import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Admin RBAC", description = "Manage users, roles, menus, button permissions, and data rules")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final TenantContext tenantContext;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminController(AdminService adminService,
                            TenantContext tenantContext,
                            JwtTokenProvider jwtTokenProvider) {
        this.adminService = adminService;
        this.tenantContext = tenantContext;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ─── Users ───

    @Operation(summary = "List users")
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.listUsers(tenantId).stream()
                .map(adminService::toUserResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return adminService.getUser(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(e -> ok(adminService.toUserResponse(e)))
                .orElse(notFound("USER_NOT_FOUND"));
    }

    @Operation(summary = "Create user")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String username = (String) body.get("username");
        if (username == null || username.isBlank()) return badRequest("USERNAME_REQUIRED");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");
        var created = adminService.createUser(tenantId, resolveUsername(request), username, email, phone, password);
        return ok(adminService.toUserResponse(created));
    }

    @Operation(summary = "Update user")
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(HttpServletRequest request, @PathVariable String id,
                                         @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String username = (String) body.get("username");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String status = (String) body.get("status");
        return adminService.updateUser(tenantId, parseUUID(id), resolveUsername(request), username, email, phone, status)
                .<ResponseEntity<?>>map(e -> ok(adminService.toUserResponse(e)))
                .orElse(notFound("USER_NOT_FOUND"));
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!adminService.deleteUser(tenantId, parseUUID(id))) return notFound("USER_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    @Operation(summary = "Assign roles to user")
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<?> assignUserRoles(HttpServletRequest request, @PathVariable String id,
                                              @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        @SuppressWarnings("unchecked")
        List<String> roleIdsRaw = (List<String>) body.get("roleIds");
        if (roleIdsRaw == null) return badRequest("ROLE_IDS_REQUIRED");
        List<UUID> roleIds = roleIdsRaw.stream().map(UUID::fromString).toList();
        if (!adminService.assignRolesToUser(tenantId, parseUUID(id), roleIds)) return notFound("USER_NOT_FOUND");
        return ok(Map.of("assigned", true));
    }

    // ─── Roles ───

    @Operation(summary = "List roles")
    @GetMapping("/roles")
    public ResponseEntity<?> listRoles(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.listRoles(tenantId).stream()
                .map(adminService::toRoleResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/roles/{id}")
    public ResponseEntity<?> getRole(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return adminService.getRole(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(e -> ok(adminService.toRoleResponse(e)))
                .orElse(notFound("ROLE_NOT_FOUND"));
    }

    @Operation(summary = "Create role")
    @PostMapping("/roles")
    public ResponseEntity<?> createRole(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String roleCode = (String) body.get("roleCode");
        String roleName = (String) body.get("roleName");
        if (roleCode == null || roleName == null) return badRequest("ROLE_CODE_AND_NAME_REQUIRED");
        String description = (String) body.get("description");
        var created = adminService.createRole(tenantId, resolveUsername(request), roleCode, roleName, description);
        return ok(adminService.toRoleResponse(created));
    }

    @Operation(summary = "Update role")
    @PutMapping("/roles/{id}")
    public ResponseEntity<?> updateRole(HttpServletRequest request, @PathVariable String id,
                                         @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String roleCode = (String) body.get("roleCode");
        String roleName = (String) body.get("roleName");
        String description = (String) body.get("description");
        String status = (String) body.get("status");
        return adminService.updateRole(tenantId, parseUUID(id), resolveUsername(request), roleCode, roleName, description, status)
                .<ResponseEntity<?>>map(e -> ok(adminService.toRoleResponse(e)))
                .orElse(notFound("ROLE_NOT_FOUND"));
    }

    @Operation(summary = "Delete role")
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> deleteRole(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!adminService.deleteRole(tenantId, parseUUID(id))) return notFound("ROLE_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    @Operation(summary = "Assign menus to role")
    @PostMapping("/roles/{id}/menus")
    public ResponseEntity<?> assignRoleMenus(HttpServletRequest request, @PathVariable String id,
                                              @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        @SuppressWarnings("unchecked")
        List<String> menuIdsRaw = (List<String>) body.get("menuIds");
        if (menuIdsRaw == null) return badRequest("MENU_IDS_REQUIRED");
        List<UUID> menuIds = menuIdsRaw.stream().map(UUID::fromString).toList();
        if (!adminService.assignMenusToRole(tenantId, parseUUID(id), menuIds)) return notFound("ROLE_NOT_FOUND");
        return ok(Map.of("assigned", true));
    }

    @Operation(summary = "Get menus assigned to role")
    @GetMapping("/roles/{id}/menus")
    public ResponseEntity<?> getRoleMenus(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<String> menuIds = adminService.getRoleMenus(tenantId, parseUUID(id)).stream()
                .map(UUID::toString).toList();
        return ok(Map.of("menuIds", menuIds));
    }

    // ─── Menus ───

    @Operation(summary = "List menus")
    @GetMapping("/menus")
    public ResponseEntity<?> listMenus(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.listMenus(tenantId).stream()
                .map(adminService::toMenuResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Get menu by ID")
    @GetMapping("/menus/{id}")
    public ResponseEntity<?> getMenu(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return adminService.getMenu(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(e -> ok(adminService.toMenuResponse(e)))
                .orElse(notFound("MENU_NOT_FOUND"));
    }

    @Operation(summary = "Create menu")
    @PostMapping("/menus")
    public ResponseEntity<?> createMenu(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) return badRequest("NAME_REQUIRED");
        String path = (String) body.get("path");
        String parentId = (String) body.get("parentId");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0;
        String icon = (String) body.get("icon");
        String type = (String) body.get("type");
        String permissionKey = (String) body.get("permissionKey");
        var created = adminService.createMenu(tenantId, resolveUsername(request), name, path,
                parentId != null ? UUID.fromString(parentId) : null, sortOrder, icon, type, permissionKey);
        return ok(adminService.toMenuResponse(created));
    }

    @Operation(summary = "Update menu")
    @PutMapping("/menus/{id}")
    public ResponseEntity<?> updateMenu(HttpServletRequest request, @PathVariable String id,
                                         @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String name = (String) body.get("name");
        String path = (String) body.get("path");
        String parentId = (String) body.get("parentId");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        String icon = (String) body.get("icon");
        String type = (String) body.get("type");
        String permissionKey = (String) body.get("permissionKey");
        String status = (String) body.get("status");
        return adminService.updateMenu(tenantId, parseUUID(id), resolveUsername(request), name, path,
                        parentId != null ? UUID.fromString(parentId) : null,
                        sortOrder, icon, type, permissionKey, status)
                .<ResponseEntity<?>>map(e -> ok(adminService.toMenuResponse(e)))
                .orElse(notFound("MENU_NOT_FOUND"));
    }

    @Operation(summary = "Delete menu")
    @DeleteMapping("/menus/{id}")
    public ResponseEntity<?> deleteMenu(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!adminService.deleteMenu(tenantId, parseUUID(id))) return notFound("MENU_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    // ─── Button Permissions ───

    @Operation(summary = "List button permissions")
    @GetMapping("/buttons")
    public ResponseEntity<?> listButtons(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.listButtons(tenantId).stream()
                .map(adminService::toButtonResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Get buttons by menu ID")
    @GetMapping("/buttons/menu/{menuId}")
    public ResponseEntity<?> getButtonsByMenu(HttpServletRequest request, @PathVariable String menuId) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.getButtonsByMenu(tenantId, parseUUID(menuId)).stream()
                .map(adminService::toButtonResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Create button permission")
    @PostMapping("/buttons")
    public ResponseEntity<?> createButton(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String menuId = (String) body.get("menuId");
        String buttonCode = (String) body.get("buttonCode");
        String buttonName = (String) body.get("buttonName");
        if (menuId == null || buttonCode == null || buttonName == null)
            return badRequest("MENU_ID_BUTTON_CODE_BUTTON_NAME_REQUIRED");
        String permissionKey = (String) body.get("permissionKey");
        var created = adminService.createButton(tenantId, resolveUsername(request), UUID.fromString(menuId),
                buttonCode, buttonName, permissionKey);
        return ok(adminService.toButtonResponse(created));
    }

    @Operation(summary = "Update button permission")
    @PutMapping("/buttons/{id}")
    public ResponseEntity<?> updateButton(HttpServletRequest request, @PathVariable String id,
                                           @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String buttonCode = (String) body.get("buttonCode");
        String buttonName = (String) body.get("buttonName");
        String permissionKey = (String) body.get("permissionKey");
        String status = (String) body.get("status");
        return adminService.updateButton(tenantId, parseUUID(id), resolveUsername(request), buttonCode, buttonName, permissionKey, status)
                .<ResponseEntity<?>>map(e -> ok(adminService.toButtonResponse(e)))
                .orElse(notFound("BUTTON_NOT_FOUND"));
    }

    @Operation(summary = "Delete button permission")
    @DeleteMapping("/buttons/{id}")
    public ResponseEntity<?> deleteButton(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!adminService.deleteButton(tenantId, parseUUID(id))) return notFound("BUTTON_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    // ─── Data Rules ───

    @Operation(summary = "List data rules")
    @GetMapping("/data-rules")
    public ResponseEntity<?> listDataRules(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.listDataRules(tenantId).stream()
                .map(adminService::toDataRuleResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Get data rules by role ID")
    @GetMapping("/data-rules/role/{roleId}")
    public ResponseEntity<?> getDataRulesByRole(HttpServletRequest request, @PathVariable String roleId) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = adminService.getDataRulesByRole(tenantId, parseUUID(roleId)).stream()
                .map(adminService::toDataRuleResponse).toList();
        return ok(result);
    }

    @Operation(summary = "Create data rule")
    @PostMapping("/data-rules")
    public ResponseEntity<?> createDataRule(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String roleId = (String) body.get("roleId");
        String resourceType = (String) body.get("resourceType");
        String ruleName = (String) body.get("ruleName");
        if (roleId == null || resourceType == null || ruleName == null)
            return badRequest("ROLE_ID_RESOURCE_TYPE_RULE_NAME_REQUIRED");
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleExpression = (Map<String, Object>) body.get("ruleExpression");
        var created = adminService.createDataRule(tenantId, resolveUsername(request), UUID.fromString(roleId),
                resourceType, ruleName, ruleExpression);
        return ok(adminService.toDataRuleResponse(created));
    }

    @Operation(summary = "Update data rule")
    @PutMapping("/data-rules/{id}")
    public ResponseEntity<?> updateDataRule(HttpServletRequest request, @PathVariable String id,
                                             @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String ruleName = (String) body.get("ruleName");
        @SuppressWarnings("unchecked")
        Map<String, Object> ruleExpression = (Map<String, Object>) body.get("ruleExpression");
        String status = (String) body.get("status");
        return adminService.updateDataRule(tenantId, parseUUID(id), resolveUsername(request), ruleName, ruleExpression, status)
                .<ResponseEntity<?>>map(e -> ok(adminService.toDataRuleResponse(e)))
                .orElse(notFound("DATA_RULE_NOT_FOUND"));
    }

    @Operation(summary = "Delete data rule")
    @DeleteMapping("/data-rules/{id}")
    public ResponseEntity<?> deleteDataRule(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!adminService.deleteDataRule(tenantId, parseUUID(id))) return notFound("DATA_RULE_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    // ─── Internal ───

    private UUID resolveTenantId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenProvider.extractTenantId(authHeader.substring(7));
        }
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null) {
            try { return UUID.fromString(tenantHeader); } catch (IllegalArgumentException ignored) {}
        }
        return tenantContext.getCurrentTenantId();
    }

    private String resolveUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenProvider.extractUsername(authHeader.substring(7));
        }
        return "system";
    }

    private UUID parseUUID(String id) { return UUID.fromString(id); }

    private ResponseEntity<?> ok(Object data) {
        return ResponseEntity.ok(new AppResponse<>(true, data, null));
    }

    private ResponseEntity<?> notFound(String code) {
        return ResponseEntity.status(404).body(new AppResponse<>(false, null, code));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(new AppResponse<>(false, null, "NOT_AUTHENTICATED"));
    }

    private ResponseEntity<?> badRequest(String code) {
        return ResponseEntity.badRequest().body(new AppResponse<>(false, null, code));
    }

    public record AppResponse<T>(boolean success, T data, String error) {}
}
