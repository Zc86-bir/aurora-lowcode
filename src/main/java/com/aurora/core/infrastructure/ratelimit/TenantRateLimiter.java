package com.aurora.core.infrastructure.ratelimit;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

/**
 * Tenant Rate Limiter — Redis-based Token Bucket via Redisson.
 *
 * Per-tenant rate limiting for AI execution endpoints:
 * - Concurrent requests: max N simultaneous per tenant (RSemaphore)
 * - Request rate: max N calls per minute per tenant (RRateLimiter)
 *
 * Key naming: aurora:semaphore:concurrent:{tenantId}
 *             aurora:ratelimit:rate:{tenantId}
 */
public class TenantRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TenantRateLimiter.class);

    private static final String SEMAPHORE_KEY_PREFIX = "aurora:semaphore:concurrent:";
    private static final String RATE_KEY_PREFIX = "aurora:ratelimit:rate:";

    private final RedissonClient redissonClient;

    // Default limits
    private final int maxConcurrentRequests;
    private final int maxRequestsPerMinute;

    public TenantRateLimiter(RedissonClient redissonClient,
                              int maxConcurrentRequests,
                              int maxRequestsPerMinute) {
        this.redissonClient = redissonClient;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    /**
     * Check if a tenant can make a request.
     * Returns true if allowed, false if rate limited.
     *
     * When allowed=false, the caller MUST NOT call release().
     * When allowed=true, the caller MUST call release() in a finally block.
     */
    public RateLimitResult tryAcquire(UUID tenantId) {
        String semaphoreKey = SEMAPHORE_KEY_PREFIX + tenantId;
        String rateKey = RATE_KEY_PREFIX + tenantId;

        // Check concurrent limit (semaphore-based)
        RSemaphore semaphore = getSemaphore(semaphoreKey);
        if (!semaphore.isExists()) {
            semaphore.trySetPermits(maxConcurrentRequests);
        }

        // Check rate limit (requests per minute)
        RRateLimiter rateLimiter = getRateLimiter(rateKey);
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(
                RateType.OVERALL,
                maxRequestsPerMinute,
                1,
                RateIntervalUnit.MINUTES
            );
        }

        // Attempt to acquire a semaphore permit (non-blocking)
        boolean concurrentOk = semaphore.tryAcquire();
        if (!concurrentOk) {
            return new RateLimitResult(false, RateLimitReason.CONCURRENT_EXCEEDED,
                maxConcurrentRequests, 0, Duration.ofSeconds(30), false);
        }

        boolean rateOk = rateLimiter.tryAcquire(1);
        if (!rateOk) {
            // DO NOT release semaphore here — let the filter handle it.
            // The caller MUST call release() even when rate check fails,
            // because the semaphore permit was already acquired above.
            return new RateLimitResult(false, RateLimitReason.RATE_EXCEEDED,
                maxRequestsPerMinute, rateLimiter.availablePermits(),
                Duration.ofSeconds(60), true);
        }

        return RateLimitResult.ALLOWED;
    }

    /**
     * Release a concurrent permit when request completes or acquisition failed after semaphore.
     * Call this in a finally block — it is safe to call even when tryAcquire returned false,
     * as long as the result.semaphoreAcquired is true.
     */
    public void release(UUID tenantId) {
        String semaphoreKey = SEMAPHORE_KEY_PREFIX + tenantId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        if (semaphore.isExists()) {
            semaphore.release();
        }
    }

    /**
     * Reset rate limits for a tenant (e.g., after plan upgrade).
     */
    public void reset(UUID tenantId) {
        String semaphoreKey = SEMAPHORE_KEY_PREFIX + tenantId;
        String rateKey = RATE_KEY_PREFIX + tenantId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.delete();
        redissonClient.getRateLimiter(rateKey).delete();
    }

    /**
     * Update limits for a tenant (e.g., tier change).
     */
    public void updateLimits(UUID tenantId, int newConcurrent, int newRatePerMinute) {
        reset(tenantId);

        String semaphoreKey = SEMAPHORE_KEY_PREFIX + tenantId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
        semaphore.trySetPermits(newConcurrent);

        String rateKey = RATE_KEY_PREFIX + tenantId;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateKey);
        rateLimiter.trySetRate(RateType.OVERALL, newRatePerMinute, 1, RateIntervalUnit.MINUTES);

        log.info("Updated rate limits for tenant {}: concurrent={}, rate={}/min",
            tenantId, newConcurrent, newRatePerMinute);
    }

    private RSemaphore getSemaphore(String key) {
        return redissonClient.getSemaphore(key);
    }

    private RRateLimiter getRateLimiter(String key) {
        return redissonClient.getRateLimiter(key);
    }

    // Value types

    public record RateLimitResult(
        boolean allowed,
        RateLimitReason reason,
        int limit,
        long remaining,
        Duration retryAfter,
        boolean semaphoreAcquired
    ) {
        public static final RateLimitResult ALLOWED =
            new RateLimitResult(true, RateLimitReason.NONE, 0, -1, Duration.ZERO, true);
    }

    public enum RateLimitReason {
        NONE,
        CONCURRENT_EXCEEDED,
        RATE_EXCEEDED,
        COST_EXCEEDED
    }
}
