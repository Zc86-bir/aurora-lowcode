package com.aurora.core.infrastructure.tenancy;

import com.aurora.core.contract.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ScopedValue-based TenantContext implementation for Java 25 virtual threads.
 *
 * <p>Uses {@link ScopedValue} for context propagation — no ThreadLocal pinning,
 * automatic cleanup on scope exit, virtual-thread-safe.
 *
 * <p>Context is set by filters via {@link #runInContext(UUID, UUID, Runnable)}
 * which creates a ScopedValue scope. All downstream code within that scope
 * can read the context via {@link #getCurrentTenantId()} etc.
 *
 * <p>For filter compatibility (where scope must outlive the filter method),
 * a ThreadLocal fallback is used. This is acceptable because the filter
 * chain runs within a single request scope and the ThreadLocal is cleaned
 * up in the finally block.
 */
@Component
public class ScopedValueTenantContext implements TenantContext {

    private static final Logger log = LoggerFactory.getLogger(ScopedValueTenantContext.class);

    // ScopedValue for reading within a scope
    private static final ScopedValue<UUID> CURRENT_TENANT = ScopedValue.newInstance();
    private static final ScopedValue<UUID> CURRENT_USER = ScopedValue.newInstance();
    private static final ScopedValue<String> CURRENT_REQUEST = ScopedValue.newInstance();

    // ThreadLocal for filter-level set/clear (scope spans the filter chain)
    private final ThreadLocal<UUID> tenantHolder = new ThreadLocal<>();
    private final ThreadLocal<UUID> userHolder = new ThreadLocal<>();
    private final ThreadLocal<String> requestHolder = new ThreadLocal<>();

    @Override
    public UUID getCurrentTenantId() {
        try {
            UUID scoped = CURRENT_TENANT.orElse(null);
            if (scoped != null) return scoped;
        } catch (Exception ignored) {}
        return tenantHolder.get();
    }

    @Override
    public UUID getCurrentUserId() {
        try {
            UUID scoped = CURRENT_USER.orElse(null);
            if (scoped != null) return scoped;
        } catch (Exception ignored) {}
        return userHolder.get();
    }

    @Override
    public TenantInfo getCurrentTenant() {
        UUID tenantId = getCurrentTenantId();
        if (tenantId == null) {
            return null;
        }
        return new TenantInfo(
                tenantId, "default", "Default Tenant",
                TenantInfo.TenantStatus.ACTIVE, TenantInfo.TenantTier.FREE,
                new TenantInfo.TenantQuota(50, 1000, 100, 10_737_418_240L, 10000, 50),
                Instant.now(), Map.of());
    }

    @Override
    public UserInfo getCurrentUser() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return new UserInfo(userId, "user-" + userId, userId + "@aurora.dev",
                Set.of("USER"), Set.of(), "Default", "en", Instant.now(), Map.of());
    }

    @Override
    public boolean isContextSet() {
        return getCurrentTenantId() != null && getCurrentUserId() != null;
    }

    @Override
    public void setContext(UUID tenantId, UUID userId) {
        tenantHolder.set(tenantId);
        userHolder.set(userId);
        log.debug("TenantContext set: tenant={}, user={}", tenantId, userId);
    }

    @Override
    public void clearContext() {
        tenantHolder.remove();
        userHolder.remove();
        requestHolder.remove();
    }

    @Override
    public <T> T runInContext(UUID tenantId, UUID userId, java.util.function.Supplier<T> action) {
        return ScopedValue.where(CURRENT_TENANT, tenantId)
                .where(CURRENT_USER, userId)
                .call(action::get);
    }

    @Override
    public void runInContext(UUID tenantId, UUID userId, Runnable action) {
        ScopedValue.where(CURRENT_TENANT, tenantId)
                .where(CURRENT_USER, userId)
                .run(action::run);
    }

    @Override
    public Map<String, Object> getTenantAttributes() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return Map.of();
    }

    @Override
    public String getRequestId() {
        return CURRENT_REQUEST.orElse(requestHolder.get());
    }

    @Override
    public void setRequestId(String requestId) {
        requestHolder.set(requestId);
    }
}
