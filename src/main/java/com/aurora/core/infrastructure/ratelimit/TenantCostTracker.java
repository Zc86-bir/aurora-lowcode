package com.aurora.core.infrastructure.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tenant Cost Tracker — per-tenant monthly LLM cost tracking with hard circuit breaker.
 *
 * Tracks cumulative cost_usd per tenant per billing cycle (monthly reset).
 * When cost exceeds the configured limit, a circuit breaker opens and all
 * AI requests for that tenant are rejected with QuotaExceededException.
 *
 * Thread-safe via AtomicLong for cost accumulation.
 */
public class TenantCostTracker {

    private static final Logger log = LoggerFactory.getLogger(TenantCostTracker.class);

    private static final long BILLING_CYCLE_DAYS = 30;

    /**
     * Per-tenant cost state.
     */
    public record TenantCostState(
        UUID tenantId,
        AtomicLong totalExecutions,
        AtomicReference<Double> totalCostUsd,
        AtomicReference<Double> monthlyCostLimit,
        AtomicReference<Instant> billingCycleStart,
        AtomicReference<Instant> lastExecutionAt,
        AtomicReference<Instant> circuitOpenedAt
    ) {
        public boolean isCircuitOpen() {
            double limit = monthlyCostLimit.get();
            double cost = totalCostUsd.get();
            return limit > 0 && cost >= limit;
        }

        public synchronized void recordCost(double cost) {
            totalCostUsd.updateAndGet(v -> v + cost);
            totalExecutions.incrementAndGet();
            lastExecutionAt.set(Instant.now());

            // Check if circuit should open — use consistent state since method is synchronized
            double currentCost = totalCostUsd.get();
            double limit = monthlyCostLimit.get();
            if (limit > 0 && currentCost >= limit && circuitOpenedAt.get() == null) {
                circuitOpenedAt.set(Instant.now());
                log.warn("COST CIRCUIT OPENED for tenant {}: ${} / ${} limit",
                    tenantId,
                    String.format("%.2f", currentCost),
                    String.format("%.2f", limit));
            }
        }

        public void resetBillingCycle() {
            totalCostUsd.set(0.0);
            totalExecutions.set(0);
            billingCycleStart.set(Instant.now());
            circuitOpenedAt.set(null);
            log.info("Billing cycle reset for tenant {}", tenantId);
        }

        public double getRemainingBudget() {
            return Math.max(0, monthlyCostLimit.get() - totalCostUsd.get());
        }

        public double getUsagePercentage() {
            double limit = monthlyCostLimit.get();
            if (limit <= 0) return 0;
            return (totalCostUsd.get() / limit) * 100.0;
        }
    }

    /**
     * Default cost limits per tier (USD per month).
     */
    public static final Map<String, Double> DEFAULT_TIER_LIMITS = Map.of(
        "FREE", 10.0,
        "STANDARD", 100.0,
        "PROFESSIONAL", 500.0,
        "ENTERPRISE", 5000.0
    );

    private final Map<UUID, TenantCostState> tenantCosts = new ConcurrentHashMap<>();
    private final Map<String, Double> tierLimits;

    public TenantCostTracker() {
        this(DEFAULT_TIER_LIMITS);
    }

    public TenantCostTracker(Map<String, Double> tierLimits) {
        this.tierLimits = Map.copyOf(tierLimits);
    }

    /**
     * Get or create cost state for a tenant.
     */
    public TenantCostState getOrCreate(UUID tenantId, String tier) {
        return tenantCosts.computeIfAbsent(tenantId, id -> {
            double limit = tierLimits.getOrDefault(tier, DEFAULT_TIER_LIMITS.get("FREE"));
            return new TenantCostState(
                id,
                new AtomicLong(0),
                new AtomicReference<>(0.0),
                new AtomicReference<>(limit),
                new AtomicReference<>(Instant.now()),
                new AtomicReference<>(null),
                new AtomicReference<>(null)
            );
        });
    }

    /**
     * Record cost for a tenant execution.
     */
    public void recordCost(UUID tenantId, double costUsd) {
        TenantCostState state = tenantCosts.get(tenantId);
        if (state != null) {
            state.recordCost(costUsd);
            checkBillingCycle(state);
        }
    }

    /**
     * Check if tenant's AI circuit is open (cost exceeded).
     */
    public boolean isCircuitOpen(UUID tenantId) {
        TenantCostState state = tenantCosts.get(tenantId);
        return state != null && state.isCircuitOpen();
    }

    /**
     * Get remaining budget for a tenant.
     */
    public double getRemainingBudget(UUID tenantId) {
        TenantCostState state = tenantCosts.get(tenantId);
        return state != null ? state.getRemainingBudget() : 0;
    }

    /**
     * Get usage percentage for a tenant.
     */
    public double getUsagePercentage(UUID tenantId) {
        TenantCostState state = tenantCosts.get(tenantId);
        return state != null ? state.getUsagePercentage() : 0;
    }

    /**
     * Update cost limit for a tenant (e.g., tier change or manual override).
     */
    public void updateLimit(UUID tenantId, double newLimit) {
        TenantCostState state = tenantCosts.get(tenantId);
        if (state != null) {
            state.monthlyCostLimit.set(newLimit);
            // If new limit is above current cost, close the circuit
            if (!state.isCircuitOpen()) {
                state.circuitOpenedAt.set(null);
            }
            log.info("Updated cost limit for tenant {}: ${}", tenantId, String.format("%.2f", newLimit));
        }
    }

    /**
     * Manually reset billing cycle for a tenant.
     */
    public void resetBillingCycle(UUID tenantId) {
        TenantCostState state = tenantCosts.get(tenantId);
        if (state != null) {
            state.resetBillingCycle();
        }
    }

    /**
     * Get cost state for all tracked tenants.
     */
    public Map<UUID, TenantCostState> getAllStates() {
        return Map.copyOf(tenantCosts);
    }

    /**
     * Check and auto-reset billing cycle if 30+ days passed.
     */
    private void checkBillingCycle(TenantCostState state) {
        Instant cycleStart = state.billingCycleStart.get();
        if (cycleStart != null && Instant.now().isAfter(cycleStart.plus(BILLING_CYCLE_DAYS, ChronoUnit.DAYS))) {
            state.resetBillingCycle();
        }
    }
}
