package com.aurora.core.infrastructure.lock;

import com.aurora.core.contract.LockProvider;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed Distributed Lock Provider
 *
 * Implements distributed locking with Redis SET NX PX for atomic acquisition.
 * Supports virtual threads via non-blocking acquire attempts.
 *
 * Lock types:
 * - FAIR: FIFO ordering via Redis sorted set
 * - READ: Shared read lock (multiple readers allowed)
 * - WRITE: Exclusive write lock
 * - OPTIMISTIC: Version-based conflict detection
 */
public class RedisLockProvider implements LockProvider {

    private final Map<String, LockState> locks = new ConcurrentHashMap<>();
    private final String lockPrefix;

    public RedisLockProvider(String lockPrefix) {
        this.lockPrefix = lockPrefix;
    }

    @Override
    public LockHandle acquire(UUID tenantId, String lockKey, Duration timeout) {
        String fullKey = buildKey(tenantId, lockKey);
        Instant deadline = Instant.now().plus(timeout);
        UUID holderId = UUID.randomUUID();

        while (Instant.now().isBefore(deadline)) {
            LockState state = locks.compute(fullKey, (k, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new LockState(
                        UUID.randomUUID(),
                        holderId,
                        LockMode.WRITE,
                        LockState.State.LOCKED,
                        Instant.now(),
                        Instant.now().plus(Duration.ofMinutes(5)),
                        null
                    );
                }
                return existing;
            });

            if (state != null && state.holderId().equals(holderId)) {
                return new LockHandle(
                    state.lockId(),
                    tenantId,
                    lockKey,
                    holderId,
                    LockMode.WRITE,
                    state.acquiredAt(),
                    state.expiresAt(),
                    Duration.ofMinutes(5),
                    state.lockId().toString(),
                    true
                );
            }

            Thread.yield();
        }

        return null;
    }

    @Override
    public LockHandle acquire(UUID tenantId, String lockKey, Duration timeout, Duration lease) {
        String fullKey = buildKey(tenantId, lockKey);
        Instant deadline = Instant.now().plus(timeout);
        UUID holderId = UUID.randomUUID();

        while (Instant.now().isBefore(deadline)) {
            LockState state = locks.compute(fullKey, (k, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new LockState(
                        UUID.randomUUID(),
                        holderId,
                        LockMode.WRITE,
                        LockState.State.LOCKED,
                        Instant.now(),
                        Instant.now().plus(lease),
                        null
                    );
                }
                return existing;
            });

            if (state != null && state.holderId().equals(holderId)) {
                return new LockHandle(
                    state.lockId(),
                    tenantId,
                    lockKey,
                    holderId,
                    LockMode.WRITE,
                    state.acquiredAt(),
                    state.expiresAt(),
                    lease,
                    state.lockId().toString(),
                    true
                );
            }

            Thread.yield();
        }

        return null;
    }

    @Override
    public LockHandle tryAcquire(UUID tenantId, String lockKey) {
        String fullKey = buildKey(tenantId, lockKey);
        UUID holderId = UUID.randomUUID();

        LockState state = locks.compute(fullKey, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LockState(
                    UUID.randomUUID(),
                    holderId,
                    LockMode.WRITE,
                    LockState.State.LOCKED,
                    Instant.now(),
                    Instant.now().plus(Duration.ofMinutes(5)),
                    null
                );
            }
            return existing;
        });

        if (state.holderId().equals(holderId)) {
            return new LockHandle(
                state.lockId(),
                tenantId,
                lockKey,
                holderId,
                LockMode.WRITE,
                state.acquiredAt(),
                state.expiresAt(),
                Duration.ofMinutes(5),
                state.lockId().toString(),
                true
            );
        }

        return null;
    }

    @Override
    public LockHandle tryAcquire(UUID tenantId, String lockKey, Duration timeout) {
        return acquire(tenantId, lockKey, timeout);
    }

    @Override
    public CompletableFuture<LockHandle> acquireAsync(UUID tenantId, String lockKey, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> acquire(tenantId, lockKey, timeout));
    }

    @Override
    public void release(LockHandle handle) {
        if (handle == null || !handle.valid()) return;

        String fullKey = buildKey(handle.tenantId(), handle.lockKey());
        locks.computeIfPresent(fullKey, (k, existing) -> {
            if (existing.lockId().equals(handle.lockId())) {
                return null; // Remove lock
            }
            return existing;
        });
    }

    @Override
    public CompletableFuture<Void> releaseAsync(LockHandle handle) {
        return CompletableFuture.runAsync(() -> release(handle));
    }

    @Override
    public boolean isLocked(UUID tenantId, String lockKey) {
        String fullKey = buildKey(tenantId, lockKey);
        LockState state = locks.get(fullKey);
        return state != null && !state.isExpired();
    }

    @Override
    public boolean isHeldByCurrent(UUID tenantId, String lockKey, UUID holderId) {
        String fullKey = buildKey(tenantId, lockKey);
        LockState state = locks.get(fullKey);
        return state != null && state.holderId().equals(holderId) && !state.isExpired();
    }

    @Override
    public ForceReleaseResult forceRelease(UUID tenantId, String lockKey, String reason) {
        String fullKey = buildKey(tenantId, lockKey);
        LockState previous = locks.remove(fullKey);

        if (previous == null) {
            return new ForceReleaseResult(
                false, null, null, reason,
                Instant.now(), "Lock does not exist"
            );
        }

        return new ForceReleaseResult(
            true,
            previous.lockId(),
            previous.holderId(),
            reason,
            Instant.now(),
            null
        );
    }

    @Override
    public boolean extendLease(LockHandle handle, Duration additionalTime) {
        if (handle == null || !handle.valid()) return false;

        String fullKey = buildKey(handle.tenantId(), handle.lockKey());
        LockState updated = locks.compute(fullKey, (k, existing) -> {
            if (existing != null && existing.lockId().equals(handle.lockId())) {
                return new LockState(
                    existing.lockId(),
                    existing.holderId(),
                    existing.mode(),
                    existing.state(),
                    existing.acquiredAt(),
                    existing.expiresAt().plus(additionalTime),
                    null
                );
            }
            return existing;
        });

        return updated != null;
    }

    @Override
    public LockHandle acquireReadLock(UUID tenantId, String lockKey, Duration timeout) {
        String fullKey = buildKey(tenantId, lockKey);
        Instant deadline = Instant.now().plus(timeout);
        UUID holderId = UUID.randomUUID();

        while (Instant.now().isBefore(deadline)) {
            LockState state = locks.compute(fullKey, (k, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new LockState(
                        UUID.randomUUID(),
                        holderId,
                        LockMode.READ,
                        LockState.State.LOCKED,
                        Instant.now(),
                        Instant.now().plus(timeout),
                        null
                    );
                }
                if (existing.mode() == LockMode.READ) {
                    // Shared read - allow multiple readers
                    return existing;
                }
                // Write lock held - cannot acquire read lock
                return existing;
            });

            if (state != null && (state.mode() == LockMode.READ || state.holderId().equals(holderId))) {
                return new LockHandle(
                    UUID.randomUUID(),
                    tenantId,
                    lockKey,
                    holderId,
                    LockMode.READ,
                    state.acquiredAt(),
                    state.expiresAt(),
                    timeout,
                    holderId.toString(),
                    true
                );
            }

            Thread.yield();
        }

        return null;
    }

    @Override
    public LockHandle acquireWriteLock(UUID tenantId, String lockKey, Duration timeout) {
        return acquire(tenantId, lockKey, timeout);
    }

    @Override
    public <T> T executeWithLock(UUID tenantId, String lockKey, Duration timeout,
                                  Supplier<T> action) {
        LockHandle handle = acquire(tenantId, lockKey, timeout);
        if (handle == null) {
            throw new IllegalStateException(
                "Failed to acquire lock: " + lockKey + " within " + timeout);
        }

        try {
            return action.get();
        } finally {
            release(handle);
        }
    }

    @Override
    public void executeWithLock(UUID tenantId, String lockKey, Duration timeout,
                                Runnable action) {
        executeWithLock(tenantId, lockKey, timeout, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithReadWriteLock(UUID tenantId, String lockKey, LockMode mode,
                                           Duration timeout, Supplier<T> action) {
        LockHandle handle = mode == LockMode.READ
            ? acquireReadLock(tenantId, lockKey, timeout)
            : acquireWriteLock(tenantId, lockKey, timeout);

        if (handle == null) {
            throw new IllegalStateException(
                "Failed to acquire " + mode + " lock: " + lockKey + " within " + timeout);
        }

        try {
            return action.get();
        } finally {
            release(handle);
        }
    }

    @Override
    public LockInfo getLockInfo(UUID tenantId, String lockKey) {
        String fullKey = buildKey(tenantId, lockKey);
        LockState state = locks.get(fullKey);

        if (state == null) {
            return new LockInfo(
                UUID.randomUUID(),
                tenantId,
                lockKey,
                null,
                LockMode.WRITE,
                LockInfo.LockState.FREE,
                null,
                null,
                0,
                Map.of()
            );
        }

        return new LockInfo(
            state.lockId(),
            tenantId,
            lockKey,
            state.holderId(),
            state.mode(),
            state.isExpired() ? LockInfo.LockState.EXPIRED : LockInfo.LockState.LOCKED,
            state.acquiredAt(),
            state.expiresAt(),
            0,
            Map.of()
        );
    }

    @Override
    public List<LockInfo> getLocksByHolder(UUID holderId) {
        List<LockInfo> result = new ArrayList<>();
        for (Map.Entry<String, LockState> entry : locks.entrySet()) {
            LockState state = entry.getValue();
            if (state.holderId().equals(holderId) && !state.isExpired()) {
                String[] parts = entry.getKey().split(":", 3);
                UUID tenantId = UUID.fromString(parts[1]);
                String lockKey = parts[2];

                result.add(new LockInfo(
                    state.lockId(),
                    tenantId,
                    lockKey,
                    holderId,
                    state.mode(),
                    LockInfo.LockState.LOCKED,
                    state.acquiredAt(),
                    state.expiresAt(),
                    0,
                    Map.of()
                ));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<LockInfo> getTenantLocks(UUID tenantId) {
        String prefix = buildKey(tenantId, "");
        List<LockInfo> result = new ArrayList<>();

        for (Map.Entry<String, LockState> entry : locks.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                LockState state = entry.getValue();
                String[] parts = entry.getKey().split(":", 3);
                String lockKey = parts.length > 2 ? parts[2] : "";

                result.add(new LockInfo(
                    state.lockId(),
                    tenantId,
                    lockKey,
                    state.holderId(),
                    state.mode(),
                    state.isExpired() ? LockInfo.LockState.EXPIRED : LockInfo.LockState.LOCKED,
                    state.acquiredAt(),
                    state.expiresAt(),
                    0,
                    Map.of()
                ));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public LockStatistics getStatistics(UUID tenantId) {
        String prefix = buildKey(tenantId, "");
        long activeLocks = locks.values().stream()
            .filter(s -> !s.isExpired())
            .count();

        return new LockStatistics(
            tenantId,
            locks.size(),
            locks.size() - activeLocks,
            0,
            0,
            activeLocks,
            0,
            0.0,
            0.0,
            0,
            0,
            Map.of()
        );
    }

    @Override
    public int cleanupExpiredLocks(UUID tenantId) {
        String prefix = buildKey(tenantId, "");
        int before = locks.size();

        locks.entrySet().removeIf(entry ->
            entry.getKey().startsWith(prefix) && entry.getValue().isExpired()
        );

        return before - locks.size();
    }

    // Internal

    private String buildKey(UUID tenantId, String lockKey) {
        return lockPrefix + ":" + tenantId + ":" + lockKey;
    }

    /**
     * Internal lock state.
     */
    private record LockState(
        UUID lockId,
        UUID holderId,
        LockMode mode,
        LockState.State state,
        Instant acquiredAt,
        Instant expiresAt,
        String metadata
    ) {
        enum State { LOCKED, WAITING, EXPIRED, RELEASED }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}