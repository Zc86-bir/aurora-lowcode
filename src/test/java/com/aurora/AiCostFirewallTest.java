package com.aurora;

import com.aurora.core.infrastructure.ratelimit.TenantCostTracker;
import com.aurora.core.infrastructure.ratelimit.TenantCostTracker.TenantCostState;
import com.aurora.core.infrastructure.ratelimit.TenantRateLimiter;
import com.aurora.core.infrastructure.ratelimit.TenantRateLimiter.RateLimitResult;
import com.aurora.core.infrastructure.ratelimit.TenantRateLimiter.RateLimitReason;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI Cost Firewall Tests
 *
 * Tests the rate limiter and cost tracker without requiring Redis.
 * Uses in-memory logic verification for the cost circuit breaker.
 */
@DisplayName("AI Cost Firewall Tests")
class AiCostFirewallTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // ============================================================
    // TenantCostTracker Tests (in-memory, no Redis)
    // ============================================================

    @Test
    @DisplayName("Tenant cost state should initialize with tier-based limit")
    void costState_shouldInitializeWithTierLimit() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0,
            "STANDARD", 100.0,
            "ENTERPRISE", 5000.0
        ));

        TenantCostState freeState = tracker.getOrCreate(TENANT_A, "FREE");
        assertEquals(10.0, freeState.monthlyCostLimit().get());
        assertEquals(0.0, freeState.totalCostUsd().get());
        assertFalse(freeState.isCircuitOpen());

        TenantCostState entState = tracker.getOrCreate(TENANT_B, "ENTERPRISE");
        assertEquals(5000.0, entState.monthlyCostLimit().get());
    }

    @Test
    @DisplayName("Circuit should open when cost exceeds limit")
    void costCircuit_shouldOpenWhenExceeded() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0
        ));

        TenantCostState state = tracker.getOrCreate(TENANT_A, "FREE");

        // Record costs below limit — circuit should stay closed
        tracker.recordCost(TENANT_A, 3.0);
        tracker.recordCost(TENANT_A, 3.0);
        tracker.recordCost(TENANT_A, 3.0);
        assertEquals(9.0, state.totalCostUsd().get());
        assertFalse(tracker.isCircuitOpen(TENANT_A));

        // Record cost that exceeds limit — circuit should open
        tracker.recordCost(TENANT_A, 2.0);
        assertEquals(11.0, state.totalCostUsd().get());
        assertTrue(tracker.isCircuitOpen(TENANT_A), "Circuit should be open after exceeding limit");
    }

    @Test
    @DisplayName("Usage percentage should be calculated correctly")
    void usagePercentage_shouldBeCorrect() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "STANDARD", 100.0
        ));

        tracker.getOrCreate(TENANT_A, "STANDARD");
        tracker.recordCost(TENANT_A, 25.0);

        assertEquals(25.0, tracker.getUsagePercentage(TENANT_A));
        assertEquals(75.0, tracker.getRemainingBudget(TENANT_A));
    }

    @Test
    @DisplayName("Billing cycle reset should clear costs and close circuit")
    void billingCycleReset_shouldClearCosts() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0
        ));

        tracker.getOrCreate(TENANT_A, "FREE");
        tracker.recordCost(TENANT_A, 15.0); // Exceed limit
        assertTrue(tracker.isCircuitOpen(TENANT_A));

        // Reset billing cycle
        tracker.resetBillingCycle(TENANT_A);

        assertFalse(tracker.isCircuitOpen(TENANT_A));
        assertEquals(0.0, tracker.getUsagePercentage(TENANT_A));
        assertEquals(10.0, tracker.getRemainingBudget(TENANT_A));
    }

    @Test
    @DisplayName("Update limit should allow higher budget and close circuit")
    void updateLimit_shouldAllowHigherBudget() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0
        ));

        tracker.getOrCreate(TENANT_A, "FREE");
        tracker.recordCost(TENANT_A, 15.0); // Exceed limit
        assertTrue(tracker.isCircuitOpen(TENANT_A));

        // Upgrade limit
        tracker.updateLimit(TENANT_A, 100.0);

        assertFalse(tracker.isCircuitOpen(TENANT_A));
        assertEquals(85.0, tracker.getRemainingBudget(TENANT_A));
    }

    @Test
    @DisplayName("Multiple tenants should have independent cost tracking")
    void multipleTenants_shouldHaveIndependentCosts() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0,
            "ENTERPRISE", 5000.0
        ));

        tracker.getOrCreate(TENANT_A, "FREE");
        tracker.getOrCreate(TENANT_B, "ENTERPRISE");

        // Tenant A exceeds free tier
        tracker.recordCost(TENANT_A, 15.0);
        assertTrue(tracker.isCircuitOpen(TENANT_A));

        // Tenant B should still be fine
        assertFalse(tracker.isCircuitOpen(TENANT_B));
        assertEquals(5000.0, tracker.getRemainingBudget(TENANT_B));
    }

    @Test
    @DisplayName("Default tier limits should be applied for unknown tier")
    void unknownTier_shouldUseFreeLimit() {
        TenantCostTracker tracker = new TenantCostTracker();

        TenantCostState state = tracker.getOrCreate(TENANT_A, "UNKNOWN");
        assertEquals(10.0, state.monthlyCostLimit().get()); // Defaults to FREE tier
    }

    @Test
    @DisplayName("All states should be retrievable")
    void getAllStates_shouldReturnAllTenants() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0,
            "ENTERPRISE", 5000.0
        ));

        tracker.getOrCreate(TENANT_A, "FREE");
        tracker.getOrCreate(TENANT_B, "ENTERPRISE");

        var states = tracker.getAllStates();
        assertEquals(2, states.size());
        assertTrue(states.containsKey(TENANT_A));
        assertTrue(states.containsKey(TENANT_B));
    }

    @Test
    @DisplayName("Cost should accumulate across multiple executions")
    void cost_shouldAccumulate() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0
        ));

        tracker.getOrCreate(TENANT_A, "FREE");

        // Simulate multiple AI calls with different costs
        tracker.recordCost(TENANT_A, 0.05);  // Small prompt
        tracker.recordCost(TENANT_A, 0.12);  // Medium generation
        tracker.recordCost(TENANT_A, 0.03);  // Quick validation
        tracker.recordCost(TENANT_A, 0.50);  // Large batch

        assertEquals(0.70, tracker.getOrCreate(TENANT_A, "FREE").totalCostUsd().get());
        assertEquals(9.30, tracker.getRemainingBudget(TENANT_A));
    }

    @Test
    @DisplayName("Circuit open log should happen only once (first breach)")
    void circuitOpen_shouldLogOnlyOnce() {
        TenantCostTracker tracker = new TenantCostTracker(Map.of(
            "FREE", 10.0
        ));

        TenantCostState state = tracker.getOrCreate(TENANT_A, "FREE");
        tracker.recordCost(TENANT_A, 11.0);
        assertNotNull(state.circuitOpenedAt().get());

        Instant firstOpenTime = state.circuitOpenedAt().get();

        // Record more cost — circuit open time should not change
        tracker.recordCost(TENANT_A, 5.0);
        assertEquals(firstOpenTime, state.circuitOpenedAt().get(),
            "Circuit opened time should not change on subsequent breaches");
    }
}
