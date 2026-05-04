package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;

/**
 * Lock Provider Interface
 *
 * Provides distributed locking with virtual thread support.
 * Supports fair locks, read-write locks, and lock leasing.
 */
public interface LockProvider {

    /**
     * Acquire a lock, blocking until available.
     */
    LockHandle acquire(UUID tenantId, String lockKey, Duration timeout);

    /**
     * Acquire a lock with lease duration.
     */
    LockHandle acquire(UUID tenantId, String lockKey, Duration timeout, Duration lease);

    /**
     * Try to acquire a lock without blocking.
     */
    LockHandle tryAcquire(UUID tenantId, String lockKey);

    /**
     * Try to acquire with timeout.
     */
    LockHandle tryAcquire(UUID tenantId, String lockKey, Duration timeout);

    /**
     * Acquire lock asynchronously.
     */
    CompletableFuture<LockHandle> acquireAsync(UUID tenantId, String lockKey, Duration timeout);

    /**
     * Release a lock.
     */
    void release(LockHandle handle);

    /**
     * Release lock asynchronously.
     */
    CompletableFuture<Void> releaseAsync(LockHandle handle);

    /**
     * Check if lock is held.
     */
    boolean isLocked(UUID tenantId, String lockKey);

    /**
     * Check if lock is held by current holder.
     */
    boolean isHeldByCurrent(UUID tenantId, String lockKey, UUID holderId);

    /**
     * Force release a lock (admin operation, requires audit).
     */
    ForceReleaseResult forceRelease(UUID tenantId, String lockKey, String reason);

    /**
     * Extend lock lease.
     */
    boolean extendLease(LockHandle handle, Duration additionalTime);

    /**
     * Acquire a read lock (shared).
     */
    LockHandle acquireReadLock(UUID tenantId, String lockKey, Duration timeout);

    /**
     * Acquire a write lock (exclusive).
     */
    LockHandle acquireWriteLock(UUID tenantId, String lockKey, Duration timeout);

    /**
     * Execute with lock (auto-release).
     */
    <T> T executeWithLock(UUID tenantId, String lockKey, Duration timeout,
                          Supplier<T> action);

    /**
     * Execute with lock (void).
     */
    void executeWithLock(UUID tenantId, String lockKey, Duration timeout,
                         Runnable action);

    /**
     * Execute with read-write lock.
     */
    <T> T executeWithReadWriteLock(UUID tenantId, String lockKey, LockMode mode,
                                    Duration timeout, Supplier<T> action);

    /**
     * Get lock information.
     */
    LockInfo getLockInfo(UUID tenantId, String lockKey);

    /**
     * Get all locks held by a holder.
     */
    List<LockInfo> getLocksByHolder(UUID holderId);

    /**
     * Get all locks for a tenant.
     */
    List<LockInfo> getTenantLocks(UUID tenantId);

    /**
     * Get lock statistics.
     */
    LockStatistics getStatistics(UUID tenantId);

    /**
     * Clean up expired locks.
     */
    int cleanupExpiredLocks(UUID tenantId);

    // Value types

    /**
     * Lock mode enum
     */
    enum LockMode {
        READ,       // Shared lock
        WRITE,      // Exclusive lock
        FAIR,       // Fair lock (FIFO)
        OPTIMISTIC  // Optimistic locking (version-based)
    }

    /**
     * Lock handle record
     */
    record LockHandle(
        UUID lockId,
        UUID tenantId,
        String lockKey,
        UUID holderId,
        LockMode mode,
        java.time.Instant acquiredAt,
        java.time.Instant expiresAt,
        Duration leaseDuration,
        String token,
        boolean valid
    ) {
        /**
         * Check if lock is still valid.
         */
        public boolean isValid() {
            return valid && java.time.Instant.now().isBefore(expiresAt);
        }

        /**
         * Get remaining lease time.
         */
        public Duration remainingLease() {
            java.time.Instant now = java.time.Instant.now();
            if (now.isAfter(expiresAt)) {
                return Duration.ZERO;
            }
            return Duration.between(now, expiresAt);
        }
    }

    /**
     * Lock information record
     */
    record LockInfo(
        UUID lockId,
        UUID tenantId,
        String lockKey,
        UUID holderId,
        LockMode mode,
        LockState state,
        java.time.Instant acquiredAt,
        java.time.Instant expiresAt,
        int waitQueueSize,
        Map<String, Object> metadata
    ) {
        public enum LockState {
            FREE, LOCKED, WAITING, EXPIRED, RELEASED
        }
    }

    /**
     * Lock statistics record
     */
    record LockStatistics(
        UUID tenantId,
        long totalAcquisitions,
        long totalReleases,
        long totalFailedAcquisitions,
        long totalTimeouts,
        long currentActiveLocks,
        long currentWaitingThreads,
        double averageAcquisitionTimeMs,
        double averageHoldTimeMs,
        long expiredLocksCleaned,
        long forceReleases,
        Map<String, Long> countByLockKey
    ) {}

    /**
     * Force release result
     */
    record ForceReleaseResult(
        boolean success,
        UUID lockId,
        UUID previousHolderId,
        String reason,
        java.time.Instant releasedAt,
        String deniedReason
    ) {}

    /**
     * Lock acquisition result
     */
    sealed interface LockAcquisitionResult permits LockAcquisitionResult.Acquired,
            LockAcquisitionResult.Failed, LockAcquisitionResult.Timeout {

        boolean isSuccess();
        String getLockKey();

        record Acquired(LockHandle handle) implements LockAcquisitionResult {
            @Override public boolean isSuccess() { return true; }
            @Override public String getLockKey() { return handle.lockKey(); }
        }

        record Failed(String lockKey, String reason) implements LockAcquisitionResult {
            @Override public boolean isSuccess() { return false; }
            @Override public String getLockKey() { return lockKey; }
        }

        record Timeout(String lockKey, Duration timeout) implements LockAcquisitionResult {
            @Override public boolean isSuccess() { return false; }
            @Override public String getLockKey() { return lockKey; }
        }
    }

    /**
     * Optimistic lock conflict
     */
    record OptimisticLockConflict(
        UUID tenantId,
        String lockKey,
        int expectedVersion,
        int actualVersion,
        UUID conflictingHolderId,
        java.time.Instant conflictAt
    ) {}
}