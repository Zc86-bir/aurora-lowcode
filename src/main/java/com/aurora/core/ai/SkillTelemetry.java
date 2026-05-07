package com.aurora.core.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Skill Telemetry &amp; Observability
 *
 * Records per-skill metrics:
 * - tokens_in / tokens_out (LLM token usage)
 * - latency_ms (end-to-end execution time)
 * - cost_usd (estimated cost based on token count)
 * - success_rate (rolling window)
 *
 * Exposes Micrometer metrics for Prometheus/Grafana dashboards.
 */
@Component
public class SkillTelemetry {

    private static final Logger log = LoggerFactory.getLogger(SkillTelemetry.class);

    // Micrometer meters
    private final Counter skillExecutionCounter;
    private final Counter skillSuccessCounter;
    private final Counter skillFailureCounter;
    private final Counter skillFallbackCounter;
    private final Counter totalTokensInCounter;
    private final Counter totalTokensOutCounter;
    private final Counter totalCostCounter;
    private final Timer skillLatencyTimer;

    // In-memory per-skill stats (for API queries)
    private final Map<String, SkillStats> skillStats = new ConcurrentHashMap<>();

    // Pricing model (per 1K tokens, as of 2026-05)
    private final double pricePer1kInputTokens;
    private final double pricePer1kOutputTokens;

    public SkillTelemetry(MeterRegistry registry, double pricePer1kInputTokens,
                           double pricePer1kOutputTokens) {
        this.pricePer1kInputTokens = pricePer1kInputTokens;
        this.pricePer1kOutputTokens = pricePer1kOutputTokens;

        // Global counters
        this.skillExecutionCounter = Counter.builder("skill.execution.total")
            .description("Total number of skill executions")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.skillSuccessCounter = Counter.builder("skill.execution.success")
            .description("Total successful skill executions")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.skillFailureCounter = Counter.builder("skill.execution.failure")
            .description("Total failed skill executions")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.skillFallbackCounter = Counter.builder("skill.execution.fallback")
            .description("Total fallback activations")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.totalTokensInCounter = Counter.builder("skill.tokens.input.total")
            .description("Total input tokens consumed")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.totalTokensOutCounter = Counter.builder("skill.tokens.output.total")
            .description("Total output tokens consumed")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.totalCostCounter = Counter.builder("skill.cost.usd.total")
            .description("Total estimated cost in USD")
            .tag("application", "aurora-lowcode")
            .register(registry);

        this.skillLatencyTimer = Timer.builder("skill.execution.latency")
            .description("Skill execution latency")
            .tag("application", "aurora-lowcode")
            .register(registry);
    }

    @Autowired
    public SkillTelemetry(MeterRegistry registry) {
        this(registry, 0.003, 0.015); // Default: Claude pricing
    }

    /**
     * Record a skill execution.
     */
    public void recordExecution(String skillId, String skillVersion,
                                 int tokensIn, int tokensOut,
                                 Duration latency, boolean success) {
        Instant now = Instant.now();

        // Update global metrics
        skillExecutionCounter.increment();
        totalTokensInCounter.increment(tokensIn);
        totalTokensOutCounter.increment(tokensOut);
        skillLatencyTimer.record(latency);

        if (success) {
            skillSuccessCounter.increment();
        } else {
            skillFailureCounter.increment();
        }

        // Estimate cost
        double cost = estimateCost(tokensIn, tokensOut);
        totalCostCounter.increment(cost);

        // Update per-skill stats
        SkillStats stats = skillStats.computeIfAbsent(skillId,
            k -> new SkillStats(skillId, skillVersion));
        stats.recordExecution(tokensIn, tokensOut, latency, success, cost, now);
    }

    /**
     * Record a fallback activation.
     */
    public void recordFallback(String skillId, String fallbackType) {
        skillFallbackCounter.increment();

        SkillStats stats = skillStats.get(skillId);
        if (stats != null) {
            stats.recordFallback(fallbackType);
        }
    }

    /**
     * Record validation outcome.
     */
    public void recordValidation(String skillId, Duration duration, boolean valid, int correctionRounds) {
        if (!valid) {
            skillFailureCounter.increment();
        }
    }

    /**
     * Record LLM call outcome (used by LlmGatewayService).
     */
    public void recordLlmCall(String provider, Duration duration, boolean success) {
        if (success) {
            skillSuccessCounter.increment();
        } else {
            skillFailureCounter.increment();
        }
        skillLatencyTimer.record(duration);
    }

    /**
     * Get per-skill statistics.
     */
    public SkillStats getSkillStats(String skillId) {
        return skillStats.get(skillId);
    }

    /**
     * Get all skill statistics.
     */
    public Map<String, SkillStats> getAllSkillStats() {
        return Map.copyOf(skillStats);
    }

    /**
     * Reset per-skill stats.
     */
    public void resetSkillStats(String skillId) {
        skillStats.remove(skillId);
    }

    // Internal

    private double estimateCost(int tokensIn, int tokensOut) {
        return (tokensIn / 1000.0) * pricePer1kInputTokens
            + (tokensOut / 1000.0) * pricePer1kOutputTokens;
    }

    /**
     * Per-skill statistics record.
     */
    public record SkillStats(
        String skillId,
        String skillVersion,
        AtomicInteger totalExecutions,
        AtomicInteger totalSuccesses,
        AtomicInteger totalFailures,
        AtomicInteger totalFallbacks,
        AtomicInteger totalTokensIn,
        AtomicInteger totalTokensOut,
        AtomicReference<Double> totalCostUsd,
        AtomicReference<Double> averageLatencyMs,
        AtomicReference<Double> successRate,
        AtomicReference<Instant> firstExecutedAt,
        AtomicReference<Instant> lastExecutedAt,
        AtomicReference<String> lastError
    ) {
        public SkillStats(String skillId, String skillVersion) {
            this(skillId, skillVersion,
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicReference<>(0.0),
                new AtomicReference<>(0.0),
                new AtomicReference<>(0.0),
                new AtomicReference<>(null),
                new AtomicReference<>(null),
                new AtomicReference<>(null));
        }

        public void recordExecution(int tokensIn, int tokensOut,
                                    Duration latency, boolean success,
                                    double cost, Instant timestamp) {
            int n = totalExecutions.incrementAndGet();
            totalTokensIn.addAndGet(tokensIn);
            totalTokensOut.addAndGet(tokensOut);
            totalCostUsd.updateAndGet(v -> v + cost);

            if (success) {
                totalSuccesses.incrementAndGet();
            } else {
                totalFailures.incrementAndGet();
            }

            // Update average latency (exponential moving average)
            double latMs = latency.toMillis();
            averageLatencyMs.updateAndGet(currentAvg -> currentAvg + (latMs - currentAvg) / n);
            successRate.updateAndGet(currentRate -> (double) totalSuccesses.get() / totalExecutions.get());

            firstExecutedAt.compareAndSet(null, timestamp);
            lastExecutedAt.set(timestamp);
        }

        public void recordFallback(String fallbackType) {
            totalFallbacks.incrementAndGet();
        }

        public void recordError(String error) {
            lastError.set(error);
        }
    }
}
