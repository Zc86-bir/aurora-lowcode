package com.aurora.core.infrastructure.database;

import com.aurora.core.contract.TenantContext;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Configures Hibernate tenant filter for row-level multi-tenancy.
 *
 * <p>Injects {@code WHERE tenant_id = :tenantId} into all queries
 * that use entities with {@code @Filter(name="tenantFilter")}.
 *
 * <p>Activation: {@code aurora.multi-tenancy.filter.enabled=true} (default: true).
 * When disabled (e.g., test profile), all rows are visible without filtering.
 */
@Component
@ConditionalOnProperty(name = "aurora.multi-tenancy.filter.enabled",
                       havingValue = "true", matchIfMissing = true)
public class TenantFilterConfigurer {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterConfigurer.class);
    private static final String TENANT_FILTER = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    private final TenantContext tenantContext;

    public TenantFilterConfigurer(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * Enable the tenant filter on the current Hibernate session.
     * Call this before any repository query to ensure tenant isolation.
     */
    public void enableFilter() {
        if (!tenantContext.isContextSet()) {
            log.debug("TenantContext not set — filter not applied");
            return;
        }

        var session = entityManager.unwrap(Session.class);
        var filter = session.enableFilter(TENANT_FILTER);
        filter.setParameter(TENANT_ID_PARAM, tenantContext.getCurrentTenantId());
        log.debug("Tenant filter enabled for tenant={}",
                tenantContext.getCurrentTenantId());
    }

    /**
     * Disable the tenant filter on the current Hibernate session.
     * Use sparingly — only for cross-tenant admin operations.
     */
    public void disableFilter() {
        var session = entityManager.unwrap(Session.class);
        session.disableFilter(TENANT_FILTER);
        log.debug("Tenant filter disabled");
    }
}
