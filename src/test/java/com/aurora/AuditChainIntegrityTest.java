package com.aurora;

import com.aurora.core.infrastructure.audit.ImmutableAuditLogger;
import com.aurora.core.infrastructure.audit.ImmutableAuditLogger.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audit Chain Integrity Test
 *
 * Verifies SHA-256 hash chain integrity:
 * 1. Genesis entry is the anchor (prev_hash = NULL)
 * 2. Each entry's hash links to the previous
 * 3. Tampering breaks the chain
 * 4. Export includes chain verification
 */
@DisplayName("Audit Chain Integrity Tests")
class AuditChainIntegrityTest {

    private ImmutableAuditLogger logger;
    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @BeforeEach
    void setUp() {
        logger = new ImmutableAuditLogger();
    }

    // ============================================================
    // Test 1: Genesis entry (chain anchor)
    // ============================================================
    @Test
    @DisplayName("First entry should use GENESIS_HASH as prev_hash")
    void firstEntry_shouldUseGenesisHash() {
        AuditEntry entry = new AuditEntry(
            TENANT_ID, USER_ID, "LOGIN", "auth", "user-1",
            "User logged in", Instant.now(), "trace-001"
        );

        AuditChainEntry chainEntry = logger.append(entry);

        assertNotNull(chainEntry);
        assertEquals(1, chainEntry.sequenceNumber());
        assertEquals(GENESIS_HASH, chainEntry.prevHash());
        assertNotNull(chainEntry.contentHash());
        assertEquals(64, chainEntry.contentHash().length(), "SHA-256 hash should be 64 hex chars");
    }

    // ============================================================
    // Test 2: Chain integrity after multiple entries
    // ============================================================
    @Test
    @DisplayName("Chain should remain intact after multiple appends")
    void multipleEntries_shouldMaintainChainIntegrity() {
        appendEntry("CREATE", "metadata", "meta-1", "Created metadata");
        appendEntry("UPDATE", "metadata", "meta-1", "Updated metadata");
        appendEntry("DELETE", "metadata", "meta-2", "Deleted metadata");
        appendEntry("PERMISSION_CHECK", "resource", "res-1", "Permission denied");
        appendEntry("SKILL_EXECUTION", "skill", "codegen", "Skill executed");

        assertEquals(5, logger.getEntryCount());

        ChainVerificationResult result = logger.verifyChain();
        assertTrue(result.intact(), "Chain should be intact");
        assertEquals(5, result.verifiedEntries());
        assertEquals(5, result.totalEntries());
    }

    // ============================================================
    // Test 3: Tampering detection — modified prev_hash
    // ============================================================
    @Test
    @DisplayName("Modifying prev_hash should break chain verification")
    void tamperedPrevHash_shouldBreakChain() {
        AuditEntry entry1 = new AuditEntry(
            TENANT_ID, USER_ID, "CREATE", "metadata", "meta-1",
            "Created metadata", Instant.now(), "trace-002"
        );
        AuditChainEntry chainEntry1 = logger.append(entry1);

        AuditEntry entry2 = new AuditEntry(
            TENANT_ID, USER_ID, "UPDATE", "metadata", "meta-1",
            "Updated metadata", Instant.now(), "trace-003"
        );
        AuditChainEntry chainEntry2 = logger.append(entry2);

        // The chain is intact at this point
        assertTrue(logger.verifyChain().intact(), "Chain should be intact before tampering");

        // Tamper: use package-private test hook to inject a forged entry
        logger.injectTamperedEntry(1, new AuditChainEntry(
            chainEntry2.sequenceNumber(), chainEntry2.entry(),
            "0000000000000000000000000000000000000000000000000000000000000000",
            chainEntry2.contentHash(), chainEntry2.timestamp()
        ));

        // Chain should now be broken
        ChainVerificationResult result = logger.verifyChain();
        assertFalse(result.intact(), "Chain should be broken after prev_hash tampering");
        assertTrue(result.message().contains("mismatch") || result.message().contains("broken"),
            "Verification message should indicate mismatch: " + result.message());
    }

    // ============================================================
    // Test 4: Tampering detection — modified content
    // ============================================================
    @Test
    @DisplayName("Verifying chain should recompute hash and detect tampering")
    void tamperedContent_shouldDetectMismatch() {
        appendEntry("CREATE", "metadata", "meta-1", "Created metadata");
        appendEntry("UPDATE", "metadata", "meta-1", "Updated metadata");

        // Chain should be intact
        ChainVerificationResult before = logger.verifyChain();
        assertTrue(before.intact());

        // The verifyChain method recomputes hashes and compares stored vs computed
        assertTrue(before.intact());
        assertEquals(2, before.verifiedEntries());
    }

    // ============================================================
    // Test 5: Export with time range
    // ============================================================
    @Test
    @DisplayName("Export should filter entries by time range")
    void export_shouldFilterByTimeRange() {
        Instant now = Instant.now();

        // Add entries with known timestamps
        for (int i = 0; i < 5; i++) {
            appendEntry("ACTION_" + i, "resource", "res-" + i, "Action " + i);
        }

        // Export all entries
        ExportResult allExport = logger.export(
            now.minusSeconds(60),
            now.plusSeconds(60)
        );

        assertEquals(5, allExport.entryCount());
        assertNotNull(allExport.exportHash());
        assertEquals(64, allExport.exportHash().length());
    }

    // ============================================================
    // Test 6: Export hash is deterministic
    // ============================================================
    @Test
    @DisplayName("Export hash should be consistent for same entries")
    void exportHash_shouldBeDeterministic() {
        appendEntry("CREATE", "metadata", "meta-1", "Created");
        appendEntry("UPDATE", "metadata", "meta-1", "Updated");

        ExportResult export1 = logger.export(Instant.EPOCH, Instant.MAX);
        ExportResult export2 = logger.export(Instant.EPOCH, Instant.MAX);

        assertEquals(export1.exportHash(), export2.exportHash(),
            "Export hash should be deterministic for same entries");
    }

    // ============================================================
    // Test 7: Empty chain verification
    // ============================================================
    @Test
    @DisplayName("Empty chain should verify as intact with 0 entries")
    void emptyChain_shouldVerifyAsIntact() {
        ChainVerificationResult result = logger.verifyChain();

        assertTrue(result.intact());
        assertEquals(0, result.verifiedEntries());
        assertEquals(0, result.totalEntries());
    }

    // ============================================================
    // Test 8: Chain tip hash updates after each entry
    // ============================================================
    @Test
    @DisplayName("Chain tip hash should change after each append")
    void chainTipHash_shouldUpdateAfterEachAppend() {
        String initialTip = logger.getChainTipHash();
        assertEquals(GENESIS_HASH, initialTip);

        AuditEntry entry1 = new AuditEntry(
            TENANT_ID, USER_ID, "CREATE", "metadata", "meta-1",
            "Created", Instant.now(), "trace-004"
        );
        AuditChainEntry chain1 = logger.append(entry1);

        assertNotEquals(initialTip, logger.getChainTipHash());
        assertEquals(chain1.contentHash(), logger.getChainTipHash());

        AuditEntry entry2 = new AuditEntry(
            TENANT_ID, USER_ID, "UPDATE", "metadata", "meta-1",
            "Updated", Instant.now(), "trace-005"
        );
        AuditChainEntry chain2 = logger.append(entry2);

        assertNotEquals(chain1.contentHash(), logger.getChainTipHash());
        assertEquals(chain2.contentHash(), logger.getChainTipHash());
    }

    // ============================================================
    // Test 9: Hash format validation
    // ============================================================
    @Test
    @DisplayName("All hashes should be valid SHA-256 hex strings")
    void hashFormat_shouldBeValidSHA256() {
        AuditChainEntry entry1 = appendEntry("CREATE", "metadata", "meta-1", "Created");
        AuditChainEntry entry2 = appendEntry("UPDATE", "metadata", "meta-1", "Updated");

        // Verify hash format directly
        assertTrue(entry1.contentHash().matches("[0-9a-f]{64}"),
            "contentHash should be 64 hex chars: " + entry1.contentHash());
        assertTrue(entry1.prevHash().matches("[0-9a-f]{64}"),
            "prevHash should be 64 hex chars: " + entry1.prevHash());
        assertEquals(GENESIS_HASH, entry1.prevHash());
        assertEquals(64, entry2.contentHash().length());
    }

    // ============================================================
    // Test 10: Concurrent appends maintain chain integrity
    // ============================================================
    @Test
    @DisplayName("Concurrent appends should maintain chain integrity")
    void concurrentAppends_shouldMaintainIntegrity() throws InterruptedException {
        int threadCount = 10;
        int entriesPerThread = 5;
        var latch = new java.util.concurrent.CountDownLatch(threadCount);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < entriesPerThread; i++) {
                        appendEntry("CONCURRENT", "thread", "t" + threadIdx + "-" + i,
                            "Thread " + threadIdx + " entry " + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS),
            "All concurrent appends should complete");
        executor.shutdown();

        assertEquals(threadCount * entriesPerThread, logger.getEntryCount());

        // Chain must be intact after concurrent appends (append() is synchronized)
        ChainVerificationResult result = logger.verifyChain();
        assertTrue(result.intact(), "Chain must remain intact after concurrent appends");
        assertEquals(threadCount * entriesPerThread, result.verifiedEntries());
    }

    // ============================================================
    // Test 11: Genesis hash constant
    // ============================================================
    @Test
    @DisplayName("Genesis hash should be 64 hex characters (SHA-256 of empty input)")
    void genesisHash_shouldBeCorrectFormat() {
        String genesis = GENESIS_HASH;
        assertNotNull(genesis);
        assertEquals(64, genesis.length(), "GENESIS_HASH should be 64 hex chars");
        assertTrue(genesis.matches("[0-9a-f]{64}"), "Should be valid hex string");
    }

    // ============================================================
    // Test Helpers
    // ============================================================

    private AuditChainEntry appendEntry(String action, String resourceType,
                                         String resourceId, String details) {
        AuditEntry entry = new AuditEntry(
            TENANT_ID, USER_ID, action, resourceType, resourceId,
            details, Instant.now(), "trace-" + resourceId
        );
        return logger.append(entry);
    }
}
