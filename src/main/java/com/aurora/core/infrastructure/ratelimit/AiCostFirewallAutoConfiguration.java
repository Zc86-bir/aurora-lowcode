package com.aurora.core.infrastructure.ratelimit;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Cost Firewall Auto-Configuration
 *
 * Wires up:
 * - RedissonClient (if not already configured)
 * - TenantRateLimiter (token bucket via Redis)
 * - TenantCostTracker (in-memory per-tenant cost tracking)
 * - AiCostFirewallFilter (Spring WebFilter for rate limiting + cost circuit breaker)
 *
 * Enabled by default. Disable via:
 *   aurora.ai-cost-firewall.enabled=false
 */
@Configuration
@ConditionalOnProperty(prefix = "aurora.ai-cost-firewall", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiCostFirewallAutoConfiguration {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${aurora.ai-cost-firewall.max-concurrent-per-tenant:5}")
    private int maxConcurrentPerTenant;

    @Value("${aurora.ai-cost-firewall.max-requests-per-minute:100}")
    private int maxRequestsPerMinute;

    @Value("${aurora.ai-cost-firewall.free-tier-limit-usd:10.0}")
    private double freeTierLimitUsd;

    @Value("${aurora.ai-cost-firewall.standard-tier-limit-usd:100.0}")
    private double standardTierLimitUsd;

    @Value("${aurora.ai-cost-firewall.professional-tier-limit-usd:500.0}")
    private double professionalTierLimitUsd;

    @Value("${aurora.ai-cost-firewall.enterprise-tier-limit-usd:5000.0}")
    private double enterpriseTierLimitUsd;

    @Bean
    @ConditionalOnProperty(prefix = "aurora.ai-cost-firewall", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
            .setAddress(address)
            .setConnectionMinimumIdleSize(2)
            .setConnectionPoolSize(10);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }

    @Bean
    public TenantCostTracker tenantCostTracker() {
        return new TenantCostTracker(java.util.Map.of(
            "FREE", freeTierLimitUsd,
            "STANDARD", standardTierLimitUsd,
            "PROFESSIONAL", professionalTierLimitUsd,
            "ENTERPRISE", enterpriseTierLimitUsd
        ));
    }

    @Bean
    public TenantRateLimiter tenantRateLimiter(RedissonClient redissonClient) {
        return new TenantRateLimiter(redissonClient, maxConcurrentPerTenant, maxRequestsPerMinute);
    }

    @Bean
    public FilterRegistrationBean<AiCostFirewallFilter> aiCostFirewallFilter(
            TenantRateLimiter rateLimiter,
            TenantCostTracker costTracker) {

        FilterRegistrationBean<AiCostFirewallFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AiCostFirewallFilter(rateLimiter, costTracker, true));
        registration.addUrlPatterns("/*");
        registration.setName("aiCostFirewallFilter");
        registration.setOrder(1); // Run early, before security filters
        return registration;
    }
}
