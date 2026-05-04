package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;

/**
 * Skill Executor Interface (MCP Compliant)
 *
 * Executes MCP-compliant skills with input/output validation,
 * sandboxing, timeout control, and fallback strategies.
 */
public interface SkillExecutor {

    /**
     * Execute a skill synchronously.
     */
    SkillResult execute(SkillRequest request);

    /**
     * Execute a skill asynchronously.
     */
    CompletableFuture<SkillResult> executeAsync(SkillRequest request);

    /**
     * Execute a skill with fallback strategy.
     */
    SkillResult executeWithFallback(SkillRequest request, FallbackStrategy fallback);

    /**
     * Validate skill input against schema.
     */
    ValidationResult validateInput(SkillRequest request);

    /**
     * Validate skill output against schema.
     */
    ValidationResult validateOutput(SkillResult result, SkillSchema outputSchema);

    /**
     * Check if skill is registered.
     */
    boolean isSkillRegistered(String skillId);

    /**
     * Get skill metadata.
     */
    SkillMetadata getSkillMetadata(String skillId);

    /**
     * Get skill schema.
     */
    SkillSchema getSkillSchema(String skillId);

    // Value types

    /**
     * Skill execution request
     */
    record SkillRequest(
        String skillId,
        UUID tenantId,
        UUID userId,
        Map<String, Object> input,
        ExecutionContext context
    ) {
        public record ExecutionContext(
            Duration timeout,
            boolean sandboxEnabled,
            boolean debugEnabled,
            Map<String, String> metadata,
            String requestId
        ) {}
    }

    /**
     * Skill execution result
     */
    sealed interface SkillResult permits SkillResult.Success, SkillResult.Failure,
            SkillResult.FallbackApplied, SkillResult.Timeout {

        String getSkillId();
        String getRequestId();
        boolean isSuccess();
        java.time.Instant getCompletedAt();
        Duration getDuration();

        record Success(
            String skillId,
            String requestId,
            Map<String, Object> output,
            java.time.Instant completedAt,
            Duration duration,
            Map<String, String> metadata
        ) implements SkillResult {
            @Override
            public boolean isSuccess() { return true; }
            @Override
            public Duration getDuration() { return duration; }
            @Override
            public java.time.Instant getCompletedAt() { return completedAt; }
            @Override
            public String getRequestId() { return requestId; }
            @Override
            public String getSkillId() { return skillId; }
        }

        record Failure(
            String skillId,
            String requestId,
            String errorCode,
            String errorMessage,
            List<String> stackTrace,
            java.time.Instant completedAt,
            Duration duration
        ) implements SkillResult {
            @Override
            public boolean isSuccess() { return false; }
            @Override
            public Duration getDuration() { return duration; }
            @Override
            public java.time.Instant getCompletedAt() { return completedAt; }
            @Override
            public String getRequestId() { return requestId; }
            @Override
            public String getSkillId() { return skillId; }
        }

        record FallbackApplied(
            String skillId,
            String requestId,
            Map<String, Object> output,
            String originalError,
            String fallbackStrategy,
            java.time.Instant completedAt,
            Duration duration
        ) implements SkillResult {
            @Override
            public boolean isSuccess() { return true; }
            @Override
            public Duration getDuration() { return duration; }
            @Override
            public java.time.Instant getCompletedAt() { return completedAt; }
            @Override
            public String getRequestId() { return requestId; }
            @Override
            public String getSkillId() { return skillId; }
        }

        record Timeout(
            String skillId,
            String requestId,
            Duration timeout,
            java.time.Instant completedAt
        ) implements SkillResult {
            @Override
            public boolean isSuccess() { return false; }
            @Override
            public Duration getDuration() { return timeout; }
            @Override
            public java.time.Instant getCompletedAt() { return completedAt; }
            @Override
            public String getRequestId() { return requestId; }
            @Override
            public String getSkillId() { return skillId; }
        }
    }

    /**
     * Skill schema definition
     */
    record SkillSchema(
        String schemaId,
        String version,
        Map<String, FieldSchema> inputSchema,
        Map<String, FieldSchema> outputSchema
    ) {
        public record FieldSchema(
            String name,
            String type,
            boolean required,
            String description,
            Object defaultValue,
            List<String> enumValues,
            Map<String, Object> constraints
        ) {}
    }

    /**
     * Skill metadata
     */
    record SkillMetadata(
        String skillId,
        String name,
        String description,
        String version,
        String executor,
        String category,
        List<String> tags,
        String author,
        java.time.Instant createdAt,
        java.time.Instant updatedAt,
        boolean deprecated,
        String successorId
    ) {}

    /**
     * Fallback strategy
     */
    sealed interface FallbackStrategy permits FallbackStrategy.Cache, FallbackStrategy.DefaultValue,
            FallbackStrategy.Graceful, FallbackStrategy.Retry, FallbackStrategy.Redirect {

        String getName();
        Duration getTimeout();

        record Cache(Duration timeout, Duration cacheTtl) implements FallbackStrategy {
            @Override public String getName() { return "CACHE"; }
            @Override public Duration getTimeout() { return timeout; }
        }

        record DefaultValue(Duration timeout, Map<String, Object> defaults) implements FallbackStrategy {
            @Override public String getName() { return "DEFAULT_VALUE"; }
            @Override public Duration getTimeout() { return timeout; }
        }

        record Graceful(Duration timeout, String message) implements FallbackStrategy {
            @Override public String getName() { return "GRACEFUL"; }
            @Override public Duration getTimeout() { return timeout; }
        }

        record Retry(Duration timeout, int maxRetries, Duration delay, double backoffMultiplier)
            implements FallbackStrategy {
            @Override public String getName() { return "RETRY"; }
            @Override public Duration getTimeout() { return timeout; }
        }

        record Redirect(Duration timeout, String fallbackSkillId) implements FallbackStrategy {
            @Override public String getName() { return "REDIRECT"; }
            @Override public Duration getTimeout() { return timeout; }
        }
    }

    /**
     * Validation result
     */
    record ValidationResult(
        boolean isValid,
        List<ValidationError> errors
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validation error
     */
    record ValidationError(
        String field,
        String code,
        String message,
        Object invalidValue
    ) {}
}