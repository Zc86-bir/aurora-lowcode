package com.aurora.core.contract;

import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.function.Supplier;
import java.time.Instant;

/**
 * Tenant Context Interface
 *
 * Holds the current tenant context for multi-tenancy support.
 * Compatible with Java 25 virtual threads (thread-local context propagation).
 */
public interface TenantContext {

    /**
     * Get the current tenant ID.
     */
    UUID getCurrentTenantId();

    /**
     * Get the current user ID.
     */
    UUID getCurrentUserId();

    /**
     * Get the current tenant information.
     */
    TenantInfo getCurrentTenant();

    /**
     * Get the current user information.
     */
    UserInfo getCurrentUser();

    /**
     * Check if tenant context is set.
     */
    boolean isContextSet();

    /**
     * Set the tenant context.
     */
    void setContext(UUID tenantId, UUID userId);

    /**
     * Clear the tenant context.
     */
    void clearContext();

    /**
     * Run code in a specific tenant context.
     */
    <T> T runInContext(UUID tenantId, UUID userId, Supplier<T> action);

    /**
     * Run code in a specific tenant context (void).
     */
    void runInContext(UUID tenantId, UUID userId, Runnable action);

    /**
     * Get tenant attributes.
     */
    Map<String, Object> getTenantAttributes();

    /**
     * Get user attributes.
     */
    Map<String, Object> getUserAttributes();

    /**
     * Get the request ID for tracing.
     */
    String getRequestId();

    /**
     * Set the request ID.
     */
    void setRequestId(String requestId);

    // Value types

    /**
     * Tenant information record
     */
    record TenantInfo(
        UUID tenantId,
        String tenantCode,
        String tenantName,
        TenantStatus status,
        TenantTier tier,
        TenantQuota quota,
        Instant createdAt,
        Map<String, Object> attributes
    ) {
        public enum TenantStatus {
            ACTIVE, SUSPENDED, DELETED, PENDING
        }

        public enum TenantTier {
            FREE, STANDARD, PROFESSIONAL, ENTERPRISE, CUSTOM
        }

        public record TenantQuota(
            int maxUsers,
            int maxMetadata,
            int maxSkills,
            long maxStorageBytes,
            int maxApiCallsPerDay,
            int maxConcurrentExecutions
        ) {}
    }

    /**
     * User information record
     */
    record UserInfo(
        UUID userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        String department,
        String locale,
        Instant lastLoginAt,
        Map<String, Object> attributes
    ) {}

    /**
     * Thread-safe context holder for virtual threads
     */
    interface ContextHolder {
        /**
         * Get the context key for thread-local storage.
         */
        String getContextKey();

        /**
         * Check if virtual threads are enabled.
         */
        boolean isVirtualThreadEnabled();

        /**
         * Get the context for propagation across threads.
         */
        ScopedContext captureContext();

        /**
         * Restore context from a scoped context.
         */
        void restoreContext(ScopedContext context);
    }

    /**
     * Scoped context record for cross-thread propagation
     */
    record ScopedContext(
        UUID tenantId,
        UUID userId,
        String requestId,
        Map<String, Object> tenantAttributes,
        Map<String, Object> userAttributes,
        Instant capturedAt
    ) {}
}