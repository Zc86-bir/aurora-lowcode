package com.aurora.core.infrastructure.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link TenantFilterInterceptor} to enable Hibernate
 * tenant filter on every request.
 */
@Configuration
public class TenantFilterWebMvcConfigurer implements WebMvcConfigurer {

    private final TenantFilterInterceptor tenantFilterInterceptor;

    public TenantFilterWebMvcConfigurer(TenantFilterInterceptor tenantFilterInterceptor) {
        this.tenantFilterInterceptor = tenantFilterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFilterInterceptor)
                .addPathPatterns("/api/**", "/mcp/**")
                .order(1);
    }
}
