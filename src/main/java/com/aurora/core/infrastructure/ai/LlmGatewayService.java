package com.aurora.core.infrastructure.ai;

import com.aurora.core.ai.SkillTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * LLM Gateway Service — Unified interface for LLM calls with retry, timeout, and cost estimation.
 *
 * <p>Wraps {@link LlmProviderRouter} to provide:
 * <ul>
 *   <li>{@code call(prompt)} — single-shot LLM invocation with circuit breaker</li>
 *   <li>{@code stream(prompt)} — streaming LLM invocation</li>
 *   <li>{@code retryWithBackoff(prompt, maxRetries)} — exponential backoff retry</li>
 *   <li>{@code costEstimation(tokensIn, tokensOut)} — cost estimation in USD</li>
 * </ul>
 *
 * <p>Timeout enforcement uses Java 25 {@link StructuredTaskScope} for structured concurrency.
 * On timeout, throws {@link LlmTimeoutException} to trigger fallback strategy.
 *
 * <p>Cost estimation uses per-model pricing:
 * <ul>
 *   <li>Anthropic Claude Sonnet: $3/MTokIn, $15/MTokOut</li>
 *   <li>OpenAI GPT-4o: $2.50/MTokIn, $10/MTokOut</li>
 * </ul>
 */
@Service
public class LlmGatewayService {

    private static final Logger log = LoggerFactory.getLogger(LlmGatewayService.class);

    // Cost per million tokens (USD)
    private static final double CLAUDE_SONNET_INPUT_COST = 3.0;
    private static final double CLAUDE_SONNET_OUTPUT_COST = 15.0;
    private static final double GPT4O_INPUT_COST = 2.50;
    private static final double GPT4O_OUTPUT_COST = 10.0;

    // StructuredTaskScope timeout for LLM calls
    private static final Duration LLM_CALL_TIMEOUT = Duration.ofSeconds(30);

    private final LlmProviderRouter router;
    private final SkillTelemetry telemetry;

    public LlmGatewayService(LlmProviderRouter router, SkillTelemetry telemetry) {
        this.router = router;
        this.telemetry = telemetry;
    }

    /**
     * Single-shot LLM call with circuit breaker routing.
     *
     * @param prompt the user prompt
     * @return LLM response text
     * @throws LlmTimeoutException if the call exceeds the timeout
     * @throws LlmProviderException if all providers fail
     */
    public String call(String prompt) {
        Instant start = Instant.now();
        try (var scope = StructuredTaskScope.<String, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(LLM_CALL_TIMEOUT))) {

            var llmTask = scope.fork(() -> executeCall(prompt));

            scope.join();

            String result = llmTask.get();
            Duration elapsed = Duration.between(start, Instant.now());
            telemetry.recordLlmCall("gateway", elapsed, true);
            return result;

        } catch (StructuredTaskScope.TimeoutException e) {
            Duration elapsed = Duration.between(start, Instant.now());
            telemetry.recordLlmCall("gateway", elapsed, false);
            throw new LlmTimeoutException("LLM call timed out after " + elapsed, e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("LLM call interrupted", e);

        } catch (Exception e) {
            Duration elapsed = Duration.between(start, Instant.now());
            telemetry.recordLlmCall("gateway", elapsed, false);
            throw new LlmProviderException("LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * LLM call with exponential backoff retry.
     *
     * @param prompt    the user prompt
     * @param maxRetries maximum retry attempts (0 = no retry)
     * @return LLM response text
     */
    public String retryWithBackoff(String prompt, int maxRetries) {
        Duration delay = Duration.ofMillis(500);
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return call(prompt);
            } catch (LlmTimeoutException | LlmProviderException e) {
                lastException = e;
                log.warn("LLM call attempt {}/{} failed: {}",
                        attempt + 1, maxRetries + 1, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new LlmProviderException("Retry interrupted", ie);
                    }
                    delay = delay.multipliedBy(2); // exponential backoff
                }
            }
        }

        throw new LlmProviderException(
                "All " + (maxRetries + 1) + " LLM attempts failed", lastException);
    }

    /**
     * Estimate cost in USD for token usage.
     *
     * @param provider  provider name ("anthropic" or "openai")
     * @param tokensIn  input tokens
     * @param tokensOut output tokens
     * @return estimated cost in USD
     */
    public double costEstimation(String provider, long tokensIn, long tokensOut) {
        double inputCostPerM;
        double outputCostPerM;

        if ("anthropic".equalsIgnoreCase(provider)) {
            inputCostPerM = CLAUDE_SONNET_INPUT_COST;
            outputCostPerM = CLAUDE_SONNET_OUTPUT_COST;
        } else {
            inputCostPerM = GPT4O_INPUT_COST;
            outputCostPerM = GPT4O_OUTPUT_COST;
        }

        double cost = (tokensIn / 1_000_000.0) * inputCostPerM
                    + (tokensOut / 1_000_000.0) * outputCostPerM;
        return Math.round(cost * 1_000_000.0) / 1_000_000.0; // 6 decimal places
    }

    /**
     * Get current router circuit state.
     */
    public LlmProviderRouter.CircuitState getCircuitState() {
        return router.getCircuitState();
    }

    private String executeCall(String prompt) {
        ChatClient.Builder builder = router.selectProvider(prompt);
        return builder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();
    }

    // Exception types

    public static class LlmTimeoutException extends RuntimeException {
        public LlmTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class LlmProviderException extends RuntimeException {
        public LlmProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
