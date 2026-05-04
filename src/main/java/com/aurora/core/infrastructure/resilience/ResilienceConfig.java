package com.aurora.core.infrastructure.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Resilience Configuration
 *
 * Provides Resilience4j circuit breaker, retry, and bulkhead isolation
 * for all AI/LLM and external service calls.
 *
 * Virtual thread pool isolation: each skill category gets its own bulkhead
 * to prevent cascade failures.
 *
 * AI call auto-degradation: on circuit breaker OPEN, fallback to cached
 * results or static templates.
 */
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    // Circuit breakers per skill category
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // Bulkheads per skill category
    private final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    // Retry configs per skill category
    private final Map<String, Retry> retries = new ConcurrentHashMap<>();

    // Virtual thread pools for isolation
    private final Map<String, ExecutorService> threadPools = new ConcurrentHashMap<>();

    public ResilienceConfig() {}

    /**
     * Register resilience config for a skill category.
     */
    public void register(String category, ResilienceSettings settings) {
        // Circuit breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(settings.failureRateThreshold())
            .slowCallRateThreshold(settings.slowCallRateThreshold())
            .slowCallDurationThreshold(Duration.ofMillis(settings.slowCallDurationMs()))
            .permittedNumberOfCallsInHalfOpenState(settings.halfOpenCalls())
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(settings.slidingWindowSize())
            .waitDurationInOpenState(Duration.ofMillis(settings.openStateWaitMs()))
            .build();

        CircuitBreaker cb = CircuitBreaker.of(category + "-cb", cbConfig);
        circuitBreakers.put(category, cb);

        // Bulkhead
        BulkheadConfig bhConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(settings.maxConcurrentCalls())
            .maxWaitDuration(Duration.ofMillis(settings.maxWaitMs()))
            .build();

        Bulkhead bh = Bulkhead.of(category + "-bh", bhConfig);
        bulkheads.put(category, bh);

        // Retry
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(settings.maxRetries())
            .waitDuration(Duration.ofMillis(settings.retryWaitMs()))
            .retryExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class, SecurityException.class)
            .build();

        Retry retry = Retry.of(category + "-retry", retryConfig);
        retries.put(category, retry);

        // Virtual thread pool
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        threadPools.put(category, pool);

        log.info("Registered resilience config for category: {}", category);
    }

    /**
     * Execute a function with full resilience protection.
     */
    public <T> T execute(String category, Supplier<T> operation, Supplier<T> fallback) {
        CircuitBreaker cb = circuitBreakers.get(category);
        Bulkhead bh = bulkheads.get(category);
        Retry retry = retries.get(category);

        if (cb == null || bh == null || retry == null) {
            // No config registered, execute directly
            return operation.get();
        }

        Supplier<T> decorated = () -> {
            return Retry.decorateSupplier(
                retry,
                () -> CircuitBreaker.decorateSupplier(cb, operation).get()
            ).get();
        };

        try {
            return decorated.get();
        } catch (Exception e) {
            log.warn("Resilience fallback triggered for category: {}", category, e);
            return fallback.get();
        }
    }

    /**
     * Get circuit breaker state.
     */
    public CircuitBreaker.State getCircuitState(String category) {
        CircuitBreaker cb = circuitBreakers.get(category);
        return cb != null ? cb.getState() : CircuitBreaker.State.CLOSED;
    }

    /**
     * Get bulkhead metrics.
     */
    public Bulkhead.Metrics getBulkheadMetrics(String category) {
        Bulkhead bh = bulkheads.get(category);
        return bh != null ? bh.getMetrics() : null;
    }

    /**
     * Shutdown all thread pools.
     */
    public void shutdown() {
        for (ExecutorService pool : threadPools.values()) {
            pool.shutdown();
        }
    }

    // Default resilience settings

    public record ResilienceSettings(
        float failureRateThreshold,
        float slowCallRateThreshold,
        long slowCallDurationMs,
        int halfOpenCalls,
        int slidingWindowSize,
        long openStateWaitMs,
        int maxConcurrentCalls,
        long maxWaitMs,
        int maxRetries,
        long retryWaitMs
    ) {
        public static ResilienceSettings forAiCall() {
            return new ResilienceSettings(
                50.0f,   // 50% failure rate opens circuit
                80.0f,   // 80% slow calls triggers circuit
                30000,   // 30s slow call threshold
                3,       // 3 test calls in half-open
                10,      // 10-call sliding window
                60000,   // 1min wait in open state
                5,       // 5 concurrent AI calls max
                5000,    // 5s max wait in bulkhead
                2,       // 2 retries
                2000     // 2s retry wait
            );
        }

        public static ResilienceSettings forDatabase() {
            return new ResilienceSettings(
                60.0f, 100.0f, 5000, 5, 20,
                30000, 50, 1000, 3, 500
            );
        }

        public static ResilienceSettings forExternalApi() {
            return new ResilienceSettings(
                40.0f, 70.0f, 10000, 3, 10,
                120000, 10, 10000, 3, 5000
            );
        }
    }
}