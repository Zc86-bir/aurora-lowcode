package com.aurora.core.infrastructure.ratelimit;

import com.aurora.core.infrastructure.ratelimit.TenantCostTracker.TenantCostState;
import com.aurora.core.infrastructure.ratelimit.TenantRateLimiter.RateLimitResult;
import com.aurora.core.infrastructure.ratelimit.TenantRateLimiter.RateLimitReason;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AI Cost Firewall Filter
 *
 * Intercepts AI execution endpoints (/api/v1/generate/*, /mcp/*) and enforces:
 * 1. Token Bucket rate limiting (per-tenant concurrent + RPM)
 * 2. Hard cost circuit breaker (per-tenant monthly USD limit)
 *
 * When rate limit is exceeded → 429 Too Many Requests
 * When cost limit is exceeded → 402 Payment Required
 */
public class AiCostFirewallFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AiCostFirewallFilter.class);

    private static final Set<String> AI_PATH_PREFIXES = Set.of(
        "/api/v1/generate/",
        "/api/v1/forms/",
        "/api/v1/reports/",
        "/api/v1/workflows/",
        "/mcp/"
    );

    private final TenantRateLimiter rateLimiter;
    private final TenantCostTracker costTracker;
    private final boolean enabled;

    public AiCostFirewallFilter(TenantRateLimiter rateLimiter,
                                 TenantCostTracker costTracker,
                                 boolean enabled) {
        this.rateLimiter = rateLimiter;
        this.costTracker = costTracker;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return AI_PATH_PREFIXES.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // Extract tenant ID from header
        String tenantIdStr = request.getHeader("X-Tenant-Id");
        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check cost circuit breaker first (fast fail)
        if (costTracker.isCircuitOpen(tenantId)) {
            TenantCostState state = costTracker.getOrCreate(tenantId, "UNKNOWN");
            sendJsonResponse(response, 402, Map.of(
                "success", "false",
                "error", "AI_QUOTA_EXCEEDED",
                "message", "Monthly AI cost limit exceeded. Please upgrade your plan or wait for the next billing cycle.",
                "currentCost", String.format("$%.2f", state.totalCostUsd().get()),
                "limit", String.format("$%.2f", state.monthlyCostLimit().get()),
                "usagePercentage", String.format("%.1f%%", state.getUsagePercentage())
            ));
            log.warn("AI request blocked for tenant {}: cost circuit open", tenantId);
            return;
        }

        // Check rate limit
        RateLimitResult result = rateLimiter.tryAcquire(tenantId);
        if (!result.allowed()) {
            int statusCode = switch (result.reason()) {
                case CONCURRENT_EXCEEDED -> 429;
                case RATE_EXCEEDED -> 429;
                case COST_EXCEEDED -> 402;
                default -> 429;
            };

            sendJsonResponse(response, statusCode, Map.of(
                "success", "false",
                "error", "RATE_LIMITED",
                "message", switch (result.reason()) {
                    case CONCURRENT_EXCEEDED ->
                        "Too many concurrent AI requests. Maximum " + result.limit() + " simultaneous requests allowed.";
                    case RATE_EXCEEDED ->
                        "AI request rate exceeded. Maximum " + result.limit() + " requests per minute.";
                    default -> "Rate limit exceeded.";
                },
                "limit", String.valueOf(result.limit()),
                "retryAfterSeconds", String.valueOf(result.retryAfter().getSeconds())
            ));

            log.debug("Rate limited tenant {}: reason={}, limit={}",
                tenantId, result.reason(), result.limit());

            // Release semaphore if it was acquired (concurrent passed but rate failed)
            if (result.semaphoreAcquired()) {
                rateLimiter.release(tenantId);
            }
            return;
        }

        // Allow request — release semaphore on completion
        try {
            filterChain.doFilter(request, response);
        } finally {
            rateLimiter.release(tenantId);
        }
    }

    private void sendJsonResponse(HttpServletResponse response, int status,
                                   Map<String, String> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        json.append("}");

        response.getWriter().write(json.toString());
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
