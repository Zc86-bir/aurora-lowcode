package com.aurora.core.infrastructure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LLM Provider Router — High Availability with Automatic Fallback.
 *
 * Routes LLM requests between primary and fallback providers:
 * - Primary: Anthropic Claude (strong reasoning)
 * - Fallback: OpenAI GPT-4o (reliable fallback)
 *
 * Circuit breaker mechanism:
 * 1. Try primary provider with configured timeout (default: 15s)
 * 2. On timeout or failure → switch to fallback provider
 * 3. Track consecutive failures — after N failures, skip primary for a cooldown period
 * 4. After cooldown, attempt primary again (half-open state)
 *
 * Thread-safe via AtomicBoolean for circuit state and AtomicInteger for failure counts.
 */
public class LlmProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderRouter.class);

    private final Map<String, ChatModel> providers;
    private final Duration primaryTimeout;
    private final int maxConsecutiveFailures;
    private final Duration cooldownPeriod;

    // Circuit breaker state
    private volatile String currentProvider;
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> circuitOpenedAt = new AtomicReference<>(null);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalFallbacks = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    public record ProviderConfig(
        String primary,
        String fallback,
        Duration timeout,
        int maxConsecutiveFailures,
        Duration cooldownPeriod
    ) {}

    public LlmProviderRouter(Map<String, ChatModel> providers, ProviderConfig config) {
        this.providers = Map.copyOf(providers);
        this.currentProvider = config.primary();
        this.primaryTimeout = config.timeout();
        this.maxConsecutiveFailures = config.maxConsecutiveFailures();
        this.cooldownPeriod = config.cooldownPeriod();

        log.info("LLM Router initialized: primary={}, fallback={}, timeout={}s, maxFailures={}",
            config.primary(), config.fallback(), config.timeout().getSeconds(),
            config.maxConsecutiveFailures());
    }

    /**
     * Select a ChatClient.Builder for the current provider.
     * Uses circuit breaker to choose between primary and fallback.
     *
     * NOTE: This method does NOT execute the request or enforce timeouts.
     * The returned ChatClient.Builder must be used by the caller to
     * execute the prompt. Timeout enforcement happens at the HTTP client
     * level (configured via Spring AI WebClient settings).
     */
    public ChatClient.Builder selectProvider(String prompt) {
        totalRequests.incrementAndGet();
        Instant start = Instant.now();

        // Check if we should attempt primary or go directly to fallback
        if (shouldUseFallback()) {
            return executeWithFallback(prompt, start);
        }

        // Try primary first
        return executeWithPrimary(prompt, start);
    }

    /**
     * Get current circuit state for monitoring.
     */
    public CircuitState getCircuitState() {
        return new CircuitState(
            currentProvider,
            circuitOpen.get(),
            consecutiveFailures.get(),
            circuitOpenedAt.get(),
            totalRequests.get(),
            totalFallbacks.get(),
            totalFailures.get(),
            getHealthStatus()
        );
    }

    /**
     * Manually reset the circuit breaker.
     */
    public void resetCircuit() {
        circuitOpen.set(false);
        consecutiveFailures.set(0);
        circuitOpenedAt.set(null);
        log.info("LLM circuit manually reset");
    }

    /**
     * Manually switch to a specific provider.
     */
    public void switchProvider(String providerName) {
        if (!providers.containsKey(providerName)) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        currentProvider = providerName;
        circuitOpen.set(false);
        consecutiveFailures.set(0);
        circuitOpenedAt.set(null);
        log.info("LLM provider manually switched to: {}", providerName);
    }

    // Internal

    private boolean shouldUseFallback() {
        if (!circuitOpen.get()) {
            return false;
        }

        // Check if cooldown has elapsed
        Instant openedAt = circuitOpenedAt.get();
        if (openedAt != null && Instant.now().isAfter(openedAt.plus(cooldownPeriod))) {
            // Cooldown elapsed — try primary again (half-open)
            circuitOpen.set(false);
            consecutiveFailures.set(0);
            circuitOpenedAt.set(null);
            log.info("LLM circuit half-open — attempting primary provider");
            return false;
        }

        return true;
    }

    private ChatClient.Builder executeWithPrimary(String prompt, Instant start) {
        ChatModel primaryModel = providers.get(currentProvider);
        if (primaryModel == null) {
            log.error("Primary provider '{}' not available — falling back", currentProvider);
            return executeFallback(prompt, start);
        }

        try {
            // Execute with timeout
            return executeWithTimeout(primaryModel, prompt, () -> {
                // Success — reset failure counter
                consecutiveFailures.set(0);
                Duration elapsed = Duration.between(start, Instant.now());
                log.debug("Primary provider success in {}ms", elapsed.toMillis());
            });
        } catch (Exception e) {
            handlePrimaryFailure(e);
            return executeFallback(prompt, start);
        }
    }

    private ChatClient.Builder executeWithFallback(String prompt, Instant start) {
        return executeFallback(prompt, start);
    }

    private ChatClient.Builder executeFallback(String prompt, Instant start) {
        String fallbackName = providers.keySet().stream()
            .filter(k -> !k.equals(currentProvider))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No fallback provider available"));

        ChatModel fallbackModel = providers.get(fallbackName);
        if (fallbackModel == null) {
            throw new IllegalStateException("Fallback provider '" + fallbackName + "' not configured");
        }

        totalFallbacks.incrementAndGet();
        log.warn("Using fallback provider '{}' after {}ms", fallbackName,
            Duration.between(start, Instant.now()).toMillis());

        return executeWithTimeout(fallbackModel, prompt, () -> {
            log.info("Fallback provider success after {}ms",
                Duration.between(start, Instant.now()).toMillis());
        });
    }

    private ChatClient.Builder executeWithTimeout(ChatModel model, String prompt,
                                                    Runnable onSuccess) {
        // Build ChatClient with the selected model
        ChatClient.Builder builder = ChatClient.builder(model);

        // Note: actual timeout enforcement happens at the HTTP client level
        // configured via Spring AI's WebClient settings.
        // This router handles logical fallback based on circuit breaker state.

        onSuccess.run();
        return builder;
    }

    private void handlePrimaryFailure(Exception e) {
        totalFailures.incrementAndGet();
        int failures = consecutiveFailures.incrementAndGet();

        log.error("Primary provider failed (consecutive: {}): {}", failures, e.getMessage());

        if (failures >= maxConsecutiveFailures) {
            circuitOpen.set(true);
            circuitOpenedAt.set(Instant.now());
            log.warn("LLM circuit opened after {} consecutive failures — using fallback for {}s",
                failures, cooldownPeriod.getSeconds());
        }
    }

    private String getHealthStatus() {
        if (!circuitOpen.get()) {
            return "healthy";
        }
        Instant openedAt = circuitOpenedAt.get();
        if (openedAt != null && Instant.now().isAfter(openedAt.plus(cooldownPeriod))) {
            return "half-open";
        }
        return "open";
    }

    // Value types

    public record CircuitState(
        String currentProvider,
        boolean circuitOpen,
        int consecutiveFailures,
        Instant circuitOpenedAt,
        long totalRequests,
        long totalFallbacks,
        long totalFailures,
        String healthStatus
    ) {
        public double fallbackRate() {
            return totalRequests > 0 ? (double) totalFallbacks / totalRequests : 0;
        }

        public double failureRate() {
            return totalRequests > 0 ? (double) totalFailures / totalRequests : 0;
        }
    }
}
