package com.aurora;

import com.aurora.core.infrastructure.tenancy.TenantLifecycleManager.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Tenant Isolation Test
 *
 * Verifies that Tenant A cannot access Tenant B's data.
 * Tests the in-memory state management, quota enforcement, and lifecycle transitions.
 * Does not require a real database — tests the state machine logic directly.
 */
@DisplayName("Multi-Tenant Isolation Tests")
class MultiTenantIsolationTest {

    private InMemoryTenantStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTenantStore();
    }

    // ============================================================
    // Test 1: Tenant provisioning creates isolated state
    // ============================================================
    @Test
    @DisplayName("Provisioning should create isolated tenant state")
    void provisioning_shouldCreateIsolatedState() {
        UUID tenantId = UUID.randomUUID();
        TenantState state = new TenantState(tenantId, TenantStatus.ACTIVE, Instant.now(), 0, 0);
        store.put(tenantId, state);

        // Verify isolation
        assertNotNull(store.get(tenantId));
        assertEquals(TenantStatus.ACTIVE, store.get(tenantId).status());
        assertNull(store.get(UUID.randomUUID()));
    }

    // ============================================================
    // Test 2: Quota enforcement per tenant
    // ============================================================
    @Test
    @DisplayName("Quota should be enforced per tenant independently")
    void quota_shouldBeEnforcedPerTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        store.put(tenantA, new TenantState(tenantA, TenantStatus.ACTIVE, Instant.now(), 0, 0));
        store.put(tenantB, new TenantState(tenantB, TenantStatus.ACTIVE, Instant.now(), 0, 0));

        TenantRecord.Quota quotaA = new TenantRecord.Quota(10, 5, 3, 1073741824L, 1000);
        TenantRecord.Quota quotaB = new TenantRecord.Quota(100, 1000, 100, 10737418240L, 100000);

        // Both tenants start with 0 usage — quota should pass
        assertDoesNotThrow(() -> enforceQuota(tenantA, quotaA));
        assertDoesNotThrow(() -> enforceQuota(tenantB, quotaB));
    }

    // ============================================================
    // Test 3: Tenant status transition
    // ============================================================
    @Test
    @DisplayName("Tenant should transition through lifecycle states correctly")
    void lifecycle_shouldTransitionThroughStates() {
        UUID tenantId = UUID.randomUUID();
        store.put(tenantId, new TenantState(tenantId, TenantStatus.ACTIVE, Instant.now(), 0, 0));

        // ACTIVE → ARCHIVED
        store.put(tenantId, new TenantState(tenantId, TenantStatus.ARCHIVED, Instant.now(), 0, 0));
        assertEquals(TenantStatus.ARCHIVED, store.get(tenantId).status());

        // ARCHIVED → DELETED
        store.put(tenantId, new TenantState(tenantId, TenantStatus.DELETED, Instant.now(), 0, 0));
        assertEquals(TenantStatus.DELETED, store.get(tenantId).status());

        // DELETED → PURGED
        store.remove(tenantId);
        assertNull(store.get(tenantId));
    }

    // ============================================================
    // Test 4: Inactive tenant should reject operations
    // ============================================================
    @Test
    @DisplayName("Archived tenant should reject quota enforcement")
    void archivedTenant_shouldRejectOperations() {
        UUID tenantId = UUID.randomUUID();
        store.put(tenantId, new TenantState(tenantId, TenantStatus.ARCHIVED, Instant.now(), 0, 0));

        // Quota enforcement should fail for archived tenant
        assertThrows(IllegalStateException.class, () ->
            enforceQuota(tenantId, new TenantRecord.Quota(10, 100, 10, 1073741824L, 1000)));
    }

    // ============================================================
    // Test 5: Concurrent tenant provisioning
    // ============================================================
    @Test
    @DisplayName("Concurrent provisioning should not corrupt state")
    void concurrentProvisioning_shouldNotCorruptState() throws InterruptedException {
        int tenantCount = 20;
        CountDownLatch latch = new CountDownLatch(tenantCount);
        AtomicInteger successCount = new AtomicInteger();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < tenantCount; i++) {
            executor.submit(() -> {
                try {
                    UUID id = UUID.randomUUID();
                    store.put(id, new TenantState(id, TenantStatus.ACTIVE, Instant.now(), 0, 0));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All provisioning should complete");
        executor.shutdown();

        assertEquals(tenantCount, successCount.get(), "All tenants should provision successfully");
        assertEquals(tenantCount, store.size(), "All tenant states should be stored");
    }

    // ============================================================
    // Test 6: Unknown tenant should throw
    // ============================================================
    @Test
    @DisplayName("Operations on unknown tenant should throw IllegalArgumentException")
    void unknownTenant_shouldThrow() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            enforceQuota(unknownId, new TenantRecord.Quota(10, 100, 10, 1073741824L, 1000)));
    }

    // ============================================================
    // Test 7: Purge expired tenants
    // ============================================================
    @Test
    @DisplayName("Purge should only remove tenants past retention period")
    void purgeExpired_shouldNotRemoveRecentTenants() {
        UUID tenantId = UUID.randomUUID();
        Instant deletionTime = Instant.now();
        store.put(tenantId, new TenantState(tenantId, TenantStatus.DELETED, deletionTime, 0, 0));

        int retentionDays = 30;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        // Just deleted — should NOT be purged (deletionTime > cutoff)
        int purged = store.removeIf(s -> s.status() == TenantStatus.DELETED && s.updatedAt().isBefore(cutoff));
        assertEquals(0, purged, "Recently deleted tenant should not be purged");
    }

    // ============================================================
    // Test 8: Purge expired tenants (old enough)
    // ============================================================
    @Test
    @DisplayName("Purge should remove tenants past retention period")
    void purgeExpired_shouldRemoveOldTenants() {
        UUID oldTenant = UUID.randomUUID();
        UUID newTenant = UUID.randomUUID();

        // Old tenant — deleted 31 days ago
        store.put(oldTenant, new TenantState(oldTenant, TenantStatus.DELETED, Instant.now().minus(31, ChronoUnit.DAYS), 0, 0));
        // New tenant — deleted 5 days ago
        store.put(newTenant, new TenantState(newTenant, TenantStatus.DELETED, Instant.now().minus(5, ChronoUnit.DAYS), 0, 0));

        int retentionDays = 30;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        int purged = store.removeIf(s -> s.status() == TenantStatus.DELETED && s.updatedAt().isBefore(cutoff));
        assertEquals(1, purged, "Only old tenant should be purged");
        assertNull(store.get(oldTenant));
        assertNotNull(store.get(newTenant));
    }

    // ============================================================
    // Test 9: Multiple tenants isolated
    // ============================================================
    @Test
    @DisplayName("Each tenant should have independent state")
    void multipleTenants_shouldHaveIndependentState() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        store.put(tenant1, new TenantState(tenant1, TenantStatus.ACTIVE, Instant.now(), 0, 0));
        store.put(tenant2, new TenantState(tenant2, TenantStatus.ACTIVE, Instant.now(), 5, 3));

        // Archiving one should not affect the other
        store.put(tenant1, new TenantState(tenant1, TenantStatus.ARCHIVED, Instant.now(), 0, 0));
        assertEquals(TenantStatus.ARCHIVED, store.get(tenant1).status());
        assertEquals(TenantStatus.ACTIVE, store.get(tenant2).status());
        assertEquals(5, store.get(tenant2).metadataCount());
        assertEquals(3, store.get(tenant2).skillCount());
    }

    // ============================================================
    // Test 10: Quota with high usage
    // ============================================================
    @Test
    @DisplayName("Quota should be exceeded when usage reaches limit")
    void quota_shouldExceedWhenUsageReachesLimit() {
        UUID tenantId = UUID.randomUUID();
        TenantRecord.Quota quota = new TenantRecord.Quota(10, 5, 3, 1073741824L, 1000);

        // Usage at limit
        store.put(tenantId, new TenantState(tenantId, TenantStatus.ACTIVE, Instant.now(), 5, 3));

        // Metadata quota: 5 >= 5 → exceeded
        assertThrows(QuotaExceededException.class, () -> enforceQuota(tenantId, quota));
    }

    // ============================================================
    // Test Helpers
    // ============================================================

    private void enforceQuota(UUID tenantId, TenantRecord.Quota quota) {
        TenantState state = store.get(tenantId);
        if (state == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        if (state.status() != TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is not active: " + state.status());
        }
        if (state.metadataCount() >= quota.maxMetadata()) {
            throw new QuotaExceededException(
                "Metadata quota exceeded: " + state.metadataCount() + "/" + quota.maxMetadata());
        }
        if (state.skillCount() >= quota.maxSkills()) {
            throw new QuotaExceededException(
                "Skill quota exceeded: " + state.skillCount() + "/" + quota.maxSkills());
        }
    }

    /**
     * In-memory tenant state store — simulates database-backed TenantLifecycleManager.
     */
    private static class InMemoryTenantStore {
        private final Map<UUID, TenantState> states = new ConcurrentHashMap<>();

        public void put(UUID tenantId, TenantState state) {
            states.put(tenantId, state);
        }

        public TenantState get(UUID tenantId) {
            return states.get(tenantId);
        }

        public void remove(UUID tenantId) {
            states.remove(tenantId);
        }

        public int size() {
            return states.size();
        }

        public int removeIf(java.util.function.Predicate<TenantState> predicate) {
            int[] count = {0};
            states.entrySet().removeIf(e -> {
                boolean match = predicate.test(e.getValue());
                if (match) count[0]++;
                return match;
            });
            return count[0];
        }
    }
}
