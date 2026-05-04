package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Cache Provider Interface
 *
 * Provides multi-level caching with tenant isolation and virtual thread support.
 * Supports L1 (local), L2 (distributed), and L3 (persistent) cache tiers.
 */
public interface CacheProvider {

    /**
     * Get value from cache.
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Get value with tenant context.
     */
    <T> Optional<T> get(UUID tenantId, String key, Class<T> type);

    /**
     * Put value into cache.
     */
    <T> void put(String key, T value, Duration ttl);

    /**
     * Put value with tenant context.
     */
    <T> void put(UUID tenantId, String key, T value, Duration ttl);

    /**
     * Put value with cache tier specification.
     */
    <T> void put(UUID tenantId, String key, T value, CacheConfig config);

    /**
     * Remove value from cache.
     */
    void evict(String key);

    /**
     * Remove value with tenant context.
     */
    void evict(UUID tenantId, String key);

    /**
     * Remove all entries matching pattern.
     */
    void evictByPattern(UUID tenantId, String pattern);

    /**
     * Clear all cache for a tenant.
     */
    void clearTenant(UUID tenantId);

    /**
     * Clear all cache (admin operation).
     */
    void clearAll();

    /**
     * Get or compute value atomically.
     */
    <T> T getOrCompute(UUID tenantId, String key, Class<T> type,
                       Supplier<T> compute, Duration ttl);

    /**
     * Get or compute value asynchronously.
     */
    <T> CompletableFuture<T> getOrComputeAsync(UUID tenantId, String key, Class<T> type,
                                                Supplier<CompletableFuture<T>> compute,
                                                Duration ttl);

    /**
     * Check if key exists.
     */
    boolean exists(UUID tenantId, String key);

    /**
     * Get cache statistics.
     */
    CacheStatistics getStatistics(UUID tenantId, CacheTier tier);

    /**
     * Get all cache statistics.
     */
    Map<CacheTier, CacheStatistics> getAllStatistics(UUID tenantId);

    /**
     * Enable/disable cache for a tenant.
     */
    void setTenantCacheEnabled(UUID tenantId, boolean enabled);

    /**
     * Get cache configuration for a tenant.
     */
    TenantCacheConfig getTenantConfig(UUID tenantId);

    /**
     * Warm up cache with initial data.
     */
    <T> void warmUp(UUID tenantId, Map<String, T> initialData, Duration ttl);

    /**
     * Invalidate stale entries.
     */
    int invalidateStale(UUID tenantId, Duration maxAge);

    // Value types

    /**
     * Cache tier enum
     */
    enum CacheTier {
        L1_LOCAL,       // In-memory local cache (per instance)
        L2_DISTRIBUTED, // Redis/Hazelcast distributed cache
        L3_PERSISTENT   // Database/file persistent cache
    }

    /**
     * Cache configuration record
     */
    record CacheConfig(
        Duration ttl,
        CacheTier tier,
        boolean compress,
        boolean encrypt,
        int maxSize,
        EvictionPolicy evictionPolicy,
        String encryptionKey
    ) {
        public enum EvictionPolicy {
            LRU, LFU, FIFO, TTL, CUSTOM
        }
    }

    /**
     * Tenant cache configuration
     */
    record TenantCacheConfig(
        UUID tenantId,
        boolean enabled,
        int maxEntriesL1,
        int maxEntriesL2,
        long maxBytesL2,
        Duration defaultTtl,
        Duration maxTtl,
        boolean compressionEnabled,
        boolean encryptionEnabled,
        Map<String, CacheConfig> customConfigs
    ) {}

    /**
     * Cache statistics record
     */
    record CacheStatistics(
        CacheTier tier,
        long totalEntries,
        long hitCount,
        long missCount,
        long evictionCount,
        long putCount,
        long getCount,
        double hitRate,
        double missRate,
        long averageLatencyNs,
        long maxLatencyNs,
        long memoryBytes,
        java.time.Instant lastResetAt
    ) {}

    /**
     * Cache entry metadata
     */
    record CacheEntryMetadata(
        String key,
        CacheTier tier,
        java.time.Instant createdAt,
        java.time.Instant expiresAt,
        java.time.Instant lastAccessedAt,
        int accessCount,
        long sizeBytes,
        boolean compressed,
        boolean encrypted,
        String checksum
    ) {}

    /**
     * Cache event for monitoring
     */
    record CacheEvent(
        UUID tenantId,
        String key,
        CacheTier tier,
        CacheEventType eventType,
        java.time.Instant timestamp,
        Duration latency,
        Map<String, Object> metadata
    ) {
        public enum CacheEventType {
            GET, PUT, EVICT, EXPIRE, HIT, MISS, COMPUTE, CLEAR, WARMUP
        }
    }
}