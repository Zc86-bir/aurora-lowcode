package com.aurora.core.contract;

import java.util.UUID;
import java.util.Set;
import java.util.Map;

/**
 * Permission Checker Interface
 *
 * Provides RBAC + ABAC hybrid permission checking for resources.
 * Supports button-level, row-level, and field-level permissions.
 */
public interface PermissionChecker {

    /**
     * Check if user has permission for an action on a resource.
     */
    boolean hasPermission(UUID userId, String resource, String action);

    /**
     * Check if user has permission with tenant context.
     */
    boolean hasPermission(UUID userId, UUID tenantId, String resource, String action);

    /**
     * Check permission with detailed result.
     */
    PermissionCheckResult checkPermission(UUID userId, UUID tenantId, String resource, String action);

    /**
     * Check data permission for row-level access.
     */
    boolean hasDataPermission(UUID userId, UUID tenantId, String resource, DataPermissionScope scope);

    /**
     * Get user's permissions for a resource.
     */
    Set<String> getUserPermissions(UUID userId, String resource);

    /**
     * Get user's roles.
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * Check if user has any of the required roles.
     */
    boolean hasAnyRole(UUID userId, Set<String> requiredRoles);

    /**
     * Check if user has all of the required roles.
     */
    boolean hasAllRoles(UUID userId, Set<String> requiredRoles);

    /**
     * Check field-level permission.
     */
    FieldPermissionResult checkFieldPermission(UUID userId, UUID tenantId,
                                               String resource, String field, String action);

    /**
     * Get visible fields for a user on a resource.
     */
    Set<String> getVisibleFields(UUID userId, UUID tenantId, String resource);

    /**
     * Get editable fields for a user on a resource.
     */
    Set<String> getEditableFields(UUID userId, UUID tenantId, String resource);

    /**
     * Apply data permission filter to a query.
     */
    String applyDataPermissionFilter(String query, UUID userId, UUID tenantId, String resource);

    /**
     * Evaluate ABAC policy.
     */
    boolean evaluatePolicy(PermissionPolicy policy, PermissionContext context);

    // Value types

    /**
     * Permission check result
     */
    record PermissionCheckResult(
        boolean allowed,
        String reason,
        Set<String> missingPermissions,
        Set<String> missingRoles,
        PermissionSource source
    ) {
        public enum PermissionSource {
            RBAC_ROLE, RBAC_PERMISSION, ABAC_POLICY, DENIED_BY_DEFAULT
        }
    }

    /**
     * Field permission result
     */
    record FieldPermissionResult(
        boolean allowed,
        FieldPermissionAction action,
        String reason,
        String maskingStrategy
    ) {
        public enum FieldPermissionAction {
            READ, WRITE, READ_MASKED, NONE
        }
    }

    /**
     * Data permission scope
     */
    enum DataPermissionScope {
        ALL,          // All data
        OWN,          // User's own data
        DEPARTMENT,   // Department data
        ORGANIZATION, // Organization data
        CUSTOM        // Custom scope defined by policy
    }

    /**
     * Permission policy (ABAC)
     */
    record PermissionPolicy(
        String policyId,
        String name,
        String description,
        String resource,
        String action,
        PolicyCondition condition,
        PolicyEffect effect,
        Map<String, Object> parameters
    ) {
        public enum PolicyEffect {
            ALLOW, DENY, ALLOW_WITH_CONDITIONS, DENY_WITH_EXCEPTIONS
        }

        public record PolicyCondition(
            String expression,
            String language,
            Map<String, String> variables
        ) {}
    }

    /**
     * Permission context for ABAC evaluation
     */
    record PermissionContext(
        UUID userId,
        UUID tenantId,
        String resource,
        String action,
        Map<String, Object> subjectAttributes,
        Map<String, Object> resourceAttributes,
        Map<String, Object> environmentAttributes
    ) {}
}