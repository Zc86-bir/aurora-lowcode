package com.aurora.core.infrastructure.database;

import com.aurora.core.contract.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that enables the Hibernate tenant filter before each request.
 *
 * <p>Registered via {@link TenantFilterWebMvcConfigurer} to run before
 * every controller method. Enables the {@code tenantFilter} on the
 * current Hibernate session so all subsequent queries include
 * {@code WHERE tenant_id = :tenantId}.
 */
@Component
public class TenantFilterInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantFilterInterceptor.class);

    private final TenantFilterConfigurer filterConfigurer;
    private final TenantContext tenantContext;

    public TenantFilterInterceptor(TenantFilterConfigurer filterConfigurer,
                                   TenantContext tenantContext) {
        this.filterConfigurer = filterConfigurer;
        this.tenantContext = tenantContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (tenantContext.isContextSet()) {
            filterConfigurer.enableFilter();
            log.trace("Hibernate tenant filter enabled for {}",
                    request.getRequestURI());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // Filter is automatically disabled when the Hibernate session closes
        // (end of request). No explicit cleanup needed.
    }
}
