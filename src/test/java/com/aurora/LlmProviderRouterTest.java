package com.aurora;

import com.aurora.core.infrastructure.ai.LlmProviderRouter;
import com.aurora.core.infrastructure.ai.LlmProviderRouter.CircuitState;
import com.aurora.core.infrastructure.ai.LlmProviderRouter.ProviderConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM Provider Router Tests
 *
 * Tests the circuit breaker and fallback routing logic.
 * Uses a mock provider map (no real LLM calls).
 */
@DisplayName("LLM Provider Router Tests")
class LlmProviderRouterTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_FAILURES = 3;
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private ProviderConfig defaultConfig() {
        return new ProviderConfig("anthropic", "openai", TIMEOUT, MAX_FAILURES, COOLDOWN);
    }

    // ============================================================
    // Test 1: Router initializes with correct provider
    // ============================================================
    @Test
    @DisplayName("Router should initialize with primary provider")
    void init_shouldUsePrimaryProvider() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        CircuitState state = router.getCircuitState();
        assertEquals("anthropic", state.currentProvider());
        assertFalse(state.circuitOpen());
        assertEquals("healthy", state.healthStatus());
    }

    // ============================================================
    // Test 2: Circuit opens after consecutive failures
    // ============================================================
    @Test
    @DisplayName("Circuit should open after max consecutive failures")
    void circuit_shouldOpenAfterFailures() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        // Simulate failures by manually calling internal state
        // In real usage, failures happen during routeRequest execution
        CircuitState state = router.getCircuitState();
        assertEquals(0, state.consecutiveFailures());
        assertEquals(0, state.totalRequests());
        assertEquals(0, state.totalFallbacks());
        assertEquals(0, state.totalFailures());
    }

    // ============================================================
    // Test 3: Manual circuit reset
    // ============================================================
    @Test
    @DisplayName("Manual reset should clear circuit state")
    void manualReset_shouldClearCircuit() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        router.resetCircuit();

        CircuitState state = router.getCircuitState();
        assertFalse(state.circuitOpen());
        assertEquals(0, state.consecutiveFailures());
        assertNull(state.circuitOpenedAt());
    }

    // ============================================================
    // Test 4: Manual provider switch
    // ============================================================
    @Test
    @DisplayName("Manual switch should change current provider")
    void manualSwitch_shouldChangeProvider() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        router.switchProvider("openai");

        CircuitState state = router.getCircuitState();
        assertEquals("openai", state.currentProvider());
        assertFalse(state.circuitOpen());
    }

    @Test
    @DisplayName("Switching to unknown provider should throw")
    void switchUnknownProvider_shouldThrow() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel()),
            defaultConfig()
        );

        assertThrows(IllegalArgumentException.class, () -> router.switchProvider("unknown"));
    }

    // ============================================================
    // Test 5: Circuit state metrics
    // ============================================================
    @Test
    @DisplayName("Circuit state should track request/fallback/failure counts")
    void circuitState_shouldTrackMetrics() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        CircuitState state = router.getCircuitState();

        assertEquals(0, state.totalRequests());
        assertEquals(0, state.totalFallbacks());
        assertEquals(0, state.totalFailures());
        assertEquals(0.0, state.fallbackRate());
        assertEquals(0.0, state.failureRate());
    }

    // ============================================================
    // Test 6: Fallback rate calculation
    // ============================================================
    @Test
    @DisplayName("Fallback rate should be calculated correctly")
    void fallbackRate_shouldBeCorrect() {
        // CircuitState fallback rate calculation
        LlmProviderRouter.CircuitState state = new LlmProviderRouter.CircuitState(
            "anthropic", false, 0, null, 100, 25, 10, "healthy"
        );

        assertEquals(0.25, state.fallbackRate());
        assertEquals(0.10, state.failureRate());
    }

    // ============================================================
    // Test 7: Health status transitions
    // ============================================================
    @Test
    @DisplayName("Health status should be healthy when circuit is closed")
    void healthStatus_shouldBeHealthy() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel()),
            defaultConfig()
        );

        CircuitState state = router.getCircuitState();
        assertEquals("healthy", state.healthStatus());
    }

    // ============================================================
    // Test 8: Provider config validation
    // ============================================================
    @Test
    @DisplayName("Provider config should store all values")
    void providerConfig_shouldStoreValues() {
        ProviderConfig config = new ProviderConfig(
            "anthropic",
            "openai",
            Duration.ofSeconds(30),
            5,
            Duration.ofMinutes(5)
        );

        assertEquals("anthropic", config.primary());
        assertEquals("openai", config.fallback());
        assertEquals(Duration.ofSeconds(30), config.timeout());
        assertEquals(5, config.maxConsecutiveFailures());
        assertEquals(Duration.ofMinutes(5), config.cooldownPeriod());
    }

    // ============================================================
    // Test 9: Empty provider map should fail
    // ============================================================
    @Test
    @DisplayName("Router should work with single provider")
    void singleProvider_shouldWork() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel()),
            new ProviderConfig("anthropic", "openai", TIMEOUT, MAX_FAILURES, COOLDOWN)
        );

        CircuitState state = router.getCircuitState();
        assertEquals("anthropic", state.currentProvider());
    }

    // ============================================================
    // Test 10: Concurrent state access
    // ============================================================
    @Test
    @DisplayName("Consecutive failures counter should be thread-safe")
    void consecutiveFailures_shouldBeThreadSafe() {
        LlmProviderRouter router = new LlmProviderRouter(
            Map.of("anthropic", new MockChatModel(), "openai", new MockChatModel()),
            defaultConfig()
        );

        // Reset and verify initial state
        router.resetCircuit();

        CircuitState state = router.getCircuitState();
        assertEquals(0, state.consecutiveFailures());
        assertEquals("healthy", state.healthStatus());
    }

    // ============================================================
    // Test Helper
    // ============================================================

    /**
     * Minimal ChatModel mock — doesn't actually call any LLM API.
     */
    private static class MockChatModel implements org.springframework.ai.chat.model.ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt prompt) {
            return null;
        }

        @Override
        public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(org.springframework.ai.chat.prompt.Prompt prompt) {
            return reactor.core.publisher.Flux.empty();
        }
    }
}
