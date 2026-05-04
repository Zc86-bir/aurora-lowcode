package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.time.Instant;

/**
 * Audit Logger Interface
 *
 * Provides comprehensive audit logging for all platform operations.
 * All logs are structured JSON and support multi-tenancy.
 */
public interface AuditLogger {

    /**
     * Log a creation event.
     */
    void logCreate(AuditEntry entry);

    /**
     * Log an update event.
     */
    void logUpdate(AuditEntry entry);

    /**
     * Log a deletion event.
     */
    void logDelete(AuditEntry entry);

    /**
     * Log a read/access event.
     */
    void logAccess(AuditEntry entry);

    /**
     * Log a permission check event.
     */
    void logPermissionCheck(PermissionAuditEntry entry);

    /**
     * Log a skill execution event.
     */
    void logSkillExecution(SkillAuditEntry entry);

    /**
     * Log a tenant switch event.
     */
    void logTenantSwitch(TenantAuditEntry entry);

    /**
     * Log a security event.
     */
    void logSecurity(SecurityAuditEntry entry);

    /**
     * Log a custom event.
     */
    void logCustom(AuditEntry entry);

    /**
     * Query audit logs.
     */
    AuditQueryResult query(AuditQuery query);

    /**
     * Get audit log by ID.
     */
    AuditLog getAuditLog(UUID auditId);

    /**
     * Export audit logs.
     */
    byte[] export(AuditQuery query, ExportFormat format);

    /**
     * Get audit statistics.
     */
    AuditStatistics getStatistics(UUID tenantId, Instant from, Instant to);

    // Value types

    /**
     * Audit type enumeration
     */
    enum AuditType {
        CREATE, UPDATE, DELETE, ACCESS, PERMISSION_CHECK,
        SKILL_EXECUTION, TENANT_SWITCH, SECURITY, CUSTOM
    }

    /**
     * Audit severity enumeration
     */
    enum AuditSeverity {
        INFO, WARN, ERROR, CRITICAL
    }

    /**
     * Base audit entry record
     */
    record AuditEntry(
        UUID auditId,
        UUID tenantId,
        UUID userId,
        String requestId,
        AuditType type,
        AuditSeverity severity,
        String resourceType,
        String resourceId,
        String action,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        Map<String, Object> metadata,
        Instant timestamp,
        String ipAddress,
        String userAgent
    ) {}

    /**
     * Permission audit entry
     */
    record PermissionAuditEntry(
        UUID auditId,
        UUID tenantId,
        UUID userId,
        String requestId,
        String resourceType,
        String resourceId,
        String action,
        boolean allowed,
        String reason,
        List<String> requiredPermissions,
        List<String> userPermissions,
        Instant timestamp,
        String ipAddress
    ) {}

    /**
     * Skill execution audit entry
     */
    record SkillAuditEntry(
        UUID auditId,
        UUID tenantId,
        UUID userId,
        String requestId,
        String skillId,
        String skillVersion,
        Map<String, Object> input,
        boolean success,
        String errorMessage,
        long durationMs,
        boolean sandboxEnabled,
        String fallbackStrategy,
        Instant timestamp
    ) {}

    /**
     * Tenant switch audit entry
     */
    record TenantAuditEntry(
        UUID auditId,
        UUID userId,
        String requestId,
        UUID previousTenantId,
        UUID newTenantId,
        String reason,
        boolean success,
        Instant timestamp,
        String ipAddress
    ) {}

    /**
     * Security audit entry
     */
    record SecurityAuditEntry(
        UUID auditId,
        UUID tenantId,
        UUID userId,
        String requestId,
        SecurityEventType eventType,
        String description,
        String resource,
        Map<String, Object> details,
        AuditSeverity severity,
        Instant timestamp,
        String ipAddress
    ) {
        public enum SecurityEventType {
            LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT,
            PERMISSION_DENIED, ACCESS_VIOLATION,
            PASSWORD_CHANGE, API_KEY_USED, TOKEN_EXPIRED,
            CSRF_VIOLATION, XSS_ATTEMPT, SQL_INJECTION_ATTEMPT,
            RATE_LIMIT_EXCEEDED, SUSPICIOUS_ACTIVITY
        }
    }

    /**
     * Audit query record
     */
    record AuditQuery(
        UUID tenantId,
        List<UUID> userIds,
        List<AuditType> types,
        List<String> resourceTypes,
        List<String> resourceIds,
        Instant from,
        Instant to,
        String searchText,
        int pageNumber,
        int pageSize,
        String sortBy,
        SortDirection sortDirection
    ) {
        public enum SortDirection { ASC, DESC }
    }

    /**
     * Audit query result
     */
    record AuditQueryResult(
        List<AuditLog> logs,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
    ) {}

    /**
     * Audit log record
     */
    record AuditLog(
        UUID auditId,
        UUID tenantId,
        UUID userId,
        String requestId,
        AuditType type,
        AuditSeverity severity,
        String resourceType,
        String resourceId,
        String action,
        String summary,
        String details,
        Instant timestamp,
        String ipAddress,
        String userAgent,
        long retentionDays
    ) {}

    /**
     * Audit statistics
     */
    record AuditStatistics(
        long totalCount,
        Map<AuditType, Long> countByType,
        Map<AuditSeverity, Long> countBySeverity,
        long permissionDeniedCount,
        long securityEventCount,
        double averageDurationMs,
        List<String> topResources,
        List<String> topActions
    ) {}

    /**
     * Export format
     */
    enum ExportFormat {
        JSON, CSV, XLSX, PDF
    }
}