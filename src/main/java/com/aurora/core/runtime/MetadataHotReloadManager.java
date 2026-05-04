package com.aurora.core.runtime;

import com.aurora.core.contract.MetadataRepository;
import com.aurora.core.contract.MetadataRepository.MetadataAggregate;
import com.aurora.core.contract.MetadataRepository.MetadataId;
import com.aurora.core.contract.MetadataRepository.MetadataStatus;
import com.aurora.core.contract.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Metadata Hot-Reload Manager
 *
 * Supports online configuration hot-update without restart.
 * Uses:
 * - Version tracking and checksum comparison
 * - Diff-based sync engine
 * - Atomic swap for hot-reload
 * - Rollback support
 */
public class MetadataHotReloadManager {

    private static final Logger log = LoggerFactory.getLogger(MetadataHotReloadManager.class);

    private final MetadataRepository metadataRepository;
    private final EventBus eventBus;

    // Current active metadata snapshots (checksum -> metadata)
    private final Map<String, MetadataSnapshot> activeSnapshots = new ConcurrentHashMap<>();

    // Reload listeners
    private final List<ReloadListener> listeners = new ArrayList<>();

    // Reload statistics
    private final AtomicLong totalReloads = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    // Diff history
    private final Map<String, List<MetadataDiff>> diffHistory = new ConcurrentHashMap<>();

    public MetadataHotReloadManager(MetadataRepository metadataRepository, EventBus eventBus) {
        this.metadataRepository = metadataRepository;
        this.eventBus = eventBus;
    }

    /**
     * Check if metadata has changed (diff sync detection).
     */
    public boolean hasChanges(UUID tenantId, String metadataName) {
        String cacheKey = buildKey(tenantId, metadataName);
        MetadataSnapshot current = activeSnapshots.get(cacheKey);

        Optional<MetadataAggregate> latest = metadataRepository.findByTenantAndName(tenantId, metadataName);
        if (latest.isEmpty()) {
            return current != null;
        }

        String newChecksum = computeChecksum(latest.get());

        if (current == null) {
            // First load
            return true;
        }

        return !current.checksum().equals(newChecksum);
    }

    /**
     * Hot-reload specific metadata without restart.
     */
    public ReloadResult hotReload(UUID tenantId, String metadataName) {
        Instant startedAt = Instant.now();
        String cacheKey = buildKey(tenantId, metadataName);

        MetadataSnapshot currentSnapshot = activeSnapshots.get(cacheKey);

        Optional<MetadataAggregate> latest = metadataRepository.findByTenantAndName(tenantId, metadataName);
        if (latest.isEmpty()) {
            activeSnapshots.remove(cacheKey);
            notifyListeners(metadataName, ReloadAction.DELETED, tenantId);
            return new ReloadResult(true, metadataName, ReloadAction.DELETED,
                Duration.between(startedAt, Instant.now()), List.of());
        }

        String newChecksum = computeChecksum(latest.get());

        // Check if actually changed
        if (currentSnapshot != null && currentSnapshot.checksum().equals(newChecksum)) {
            return new ReloadResult(false, metadataName, ReloadAction.NO_CHANGE,
                Duration.between(startedAt, Instant.now()),
                List.of(new ReloadWarning("No changes detected")));
        }

        try {
            // Compute diff
            MetadataDiff diff = computeDiff(currentSnapshot, latest.get());

            // Atomic swap
            MetadataSnapshot newSnapshot = new MetadataSnapshot(
                latest.get().getId(),
                newChecksum,
                latest.get().getVersion(),
                latest.get().getStatus(),
                Instant.now()
            );

            activeSnapshots.put(cacheKey, newSnapshot);

            // Record diff
            diffHistory.computeIfAbsent(cacheKey, k -> new ArrayList<>()).add(diff);

            totalReloads.incrementAndGet();

            // Notify listeners
            ReloadAction action = currentSnapshot == null ? ReloadAction.CREATED : ReloadAction.UPDATED;
            notifyListeners(metadataName, action, tenantId);

            return new ReloadResult(true, metadataName, action,
                Duration.between(startedAt, Instant.now()), List.copyOf(diff.entries()));

        } catch (Exception e) {
            totalFailures.incrementAndGet();
            return new ReloadResult(false, metadataName, ReloadAction.ERROR,
                Duration.between(startedAt, Instant.now()),
                List.of(new ReloadError(e.getMessage())));
        }
    }

    /**
     * Hot-reload all metadata for a tenant.
     */
    public BulkReloadResult hotReloadAll(UUID tenantId) {
        Instant startedAt = Instant.now();
        List<ReloadResult> results = new ArrayList<>();

        List<MetadataAggregate> allMetadata = metadataRepository.findByTenant(tenantId);

        for (MetadataAggregate metadata : allMetadata) {
            ReloadResult result = hotReload(tenantId, metadata.getName());
            results.add(result);
        }

        long success = results.stream().filter(ReloadResult::success).count();
        long failed = results.size() - success;

        return new BulkReloadResult(
            tenantId,
            results.size(),
            success,
            failed,
            Duration.between(startedAt, Instant.now()),
            results
        );
    }

    /**
     * Rollback to a previous version.
     */
    public RollbackResult rollback(UUID tenantId, String metadataName, int targetVersion) {
        Instant startedAt = Instant.now();
        String cacheKey = buildKey(tenantId, metadataName);

        // Find target version in repository
        MetadataId id = activeSnapshots.get(cacheKey) != null
            ? activeSnapshots.get(cacheKey).metadataId()
            : null;

        if (id == null) {
            return new RollbackResult(false, metadataName, targetVersion,
                "No active snapshot found", Duration.ZERO);
        }

        Optional<MetadataAggregate> target = metadataRepository.findByIdAndVersion(id, targetVersion);
        if (target.isEmpty()) {
            return new RollbackResult(false, metadataName, targetVersion,
                "Version " + targetVersion + " not found", Duration.ZERO);
        }

        try {
            MetadataAggregate rolledBack = metadataRepository.rollback(id, targetVersion);
            String checksum = computeChecksum(rolledBack);

            activeSnapshots.put(cacheKey, new MetadataSnapshot(
                rolledBack.getId(),
                checksum,
                targetVersion,
                rolledBack.getStatus(),
                Instant.now()
            ));

            notifyListeners(metadataName, ReloadAction.ROLLBACK, tenantId);

            return new RollbackResult(true, metadataName, targetVersion,
                null, Duration.between(startedAt, Instant.now()));

        } catch (Exception e) {
            return new RollbackResult(false, metadataName, targetVersion,
                e.getMessage(), Duration.between(startedAt, Instant.now()));
        }
    }

    /**
     * Get diff history for a metadata.
     */
    public List<MetadataDiff> getDiffHistory(UUID tenantId, String metadataName) {
        String cacheKey = buildKey(tenantId, metadataName);
        return diffHistory.getOrDefault(cacheKey, List.of());
    }

    /**
     * Get active snapshot info.
     */
    public Optional<MetadataSnapshot> getActiveSnapshot(UUID tenantId, String metadataName) {
        return Optional.ofNullable(activeSnapshots.get(buildKey(tenantId, metadataName)));
    }

    /**
     * Register a reload listener.
     */
    public void addListener(ReloadListener listener) {
        listeners.add(listener);
    }

    /**
     * Get reload statistics.
     */
    public ReloadStats getStats() {
        return new ReloadStats(
            totalReloads.get(),
            totalFailures.get(),
            activeSnapshots.size(),
            diffHistory.values().stream().mapToInt(List::size).sum()
        );
    }

    /**
     * Clear all caches (cold restart).
     */
    public void clearAll() {
        activeSnapshots.clear();
        diffHistory.clear();
    }

    // Internal

    private String buildKey(UUID tenantId, String metadataName) {
        return tenantId + ":" + metadataName;
    }

    private String computeChecksum(MetadataAggregate metadata) {
        try {
            String content = metadata.getName() + metadata.getVersion() + metadata.getStatus()
                + metadata.getUpdatedAt();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "unknown";
        }
    }

    private MetadataDiff computeDiff(MetadataSnapshot oldSnapshot, MetadataAggregate newMetadata) {
        List<MetadataDiff.DiffEntry> entries = new ArrayList<>();

        if (oldSnapshot == null) {
            entries.add(new MetadataDiff.DiffEntry("metadata", DiffType.ADD, null, newMetadata.getName()));
            return new MetadataDiff(0, newMetadata.getVersion(), List.copyOf(entries), Instant.now());
        }

        if (oldSnapshot.version() != newMetadata.getVersion()) {
            entries.add(new MetadataDiff.DiffEntry("version", DiffType.MODIFY,
                oldSnapshot.version(), newMetadata.getVersion()));
        }

        if (oldSnapshot.status() != newMetadata.getStatus()) {
            entries.add(new MetadataDiff.DiffEntry("status", DiffType.MODIFY,
                oldSnapshot.status().name(), newMetadata.getStatus().name()));
        }

        if (!oldSnapshot.checksum().equals(computeChecksum(newMetadata))) {
            entries.add(new MetadataDiff.DiffEntry("content", DiffType.MODIFY,
                oldSnapshot.checksum(), computeChecksum(newMetadata)));
        }

        return new MetadataDiff(
            oldSnapshot.version(),
            newMetadata.getVersion(),
            List.copyOf(entries),
            Instant.now()
        );
    }

    private void notifyListeners(String metadataName, ReloadAction action, UUID tenantId) {
        for (ReloadListener listener : listeners) {
            try {
                listener.onReload(metadataName, action, tenantId);
            } catch (Exception e) {
                // Log listener errors but don't break the reload
                log.warn("Reload listener error for {}: {}", metadataName, e.getMessage());
            }
        }
    }

    // Value types

    public enum ReloadAction {
        CREATED, UPDATED, DELETED, ROLLBACK, ERROR, NO_CHANGE
    }

    public enum DiffType {
        ADD, REMOVE, MODIFY
    }

    public record MetadataSnapshot(
        MetadataId metadataId,
        String checksum,
        int version,
        MetadataStatus status,
        Instant lastLoadedAt
    ) {}

    public record ReloadResult(
        boolean success,
        String metadataName,
        ReloadAction action,
        Duration duration,
        List<Object> details
    ) {}

    public record BulkReloadResult(
        UUID tenantId,
        int total,
        long success,
        long failed,
        Duration duration,
        List<ReloadResult> results
    ) {}

    public record RollbackResult(
        boolean success,
        String metadataName,
        int rolledBackToVersion,
        String errorMessage,
        Duration duration
    ) {}

    public record MetadataDiff(
        int fromVersion,
        int toVersion,
        List<DiffEntry> entries,
        Instant computedAt
    ) {
        public record DiffEntry(
            String field,
            DiffType type,
            Object oldValue,
            Object newValue
        ) {}
    }

    public record ReloadStats(
        long totalReloads,
        long totalFailures,
        int cachedItems,
        int diffEntries
    ) {}

    public record ReloadWarning(String message) {}
    public record ReloadError(String message) {}

    @FunctionalInterface
    public interface ReloadListener {
        void onReload(String metadataName, ReloadAction action, UUID tenantId);
    }
}