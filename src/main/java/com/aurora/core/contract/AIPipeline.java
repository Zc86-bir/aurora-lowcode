package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.time.Duration;

/**
 * AI Pipeline Interface
 *
 * Orchestrates the AI generation pipeline with structured validation,
 * generation, and rollback capabilities.
 *
 * Pipeline stages:
 * 1. Prompt Building
 * 2. LLM Generation
 * 3. JSON Schema Validation
 * 4. Business Rule Check
 * 5. Code/Config Generation
 * 6. Static Analysis (SpotBugs/ESLint)
 * 7. Unit Test Scaffold Generation
 * 8. Git Commit
 */
public interface AIPipeline {

    /**
     * Execute the full AI pipeline.
     */
    PipelineResult execute(PipelineRequest request);

    /**
     * Execute the pipeline asynchronously with structured concurrency.
     */
    CompletableFuture<PipelineResult> executeAsync(PipelineRequest request);

    /**
     * Execute only validation stages.
     */
    ValidationResult validateOnly(PipelineRequest request);

    /**
     * Execute generation with rollback on failure.
     */
    PipelineResult executeWithRollback(PipelineRequest request);

    /**
     * Execute parallel validation stages using structured task scope.
     */
    ValidationResult executeParallelValidation(PipelineRequest request);

    /**
     * Rollback to a previous state.
     */
    RollbackResult rollback(String pipelineId, int targetStage);

    /**
     * Get pipeline execution status.
     */
    PipelineStatus getStatus(String pipelineId);

    /**
     * Get pipeline metrics.
     */
    PipelineMetrics getMetrics(String pipelineId);

    // Value types

    /**
     * Pipeline execution request
     */
    record PipelineRequest(
        String pipelineId,
        UUID tenantId,
        UUID userId,
        String prompt,
        PromptConfig promptConfig,
        ValidationConfig validationConfig,
        GenerationConfig generationConfig,
        OutputConfig outputConfig
    ) {
        public record PromptConfig(
            String modelId,
            double temperature,
            int maxTokens,
            String systemPrompt,
            List<String> examples,
            String language
        ) {}

        public record ValidationConfig(
            boolean schemaValidationEnabled,
            boolean businessRuleCheckEnabled,
            boolean staticAnalysisEnabled,
            boolean testGenerationEnabled,
            String schemaPath,
            List<String> businessRules
        ) {}

        public record GenerationConfig(
            String outputType,
            String templatePath,
            String packagePrefix,
            boolean enableComments,
            boolean enableSwagger
        ) {}

        public record OutputConfig(
            String outputPath,
            boolean createGitCommit,
            String commitMessage,
            boolean enableRollback
        ) {}
    }

    /**
     * Pipeline execution result
     */
    sealed interface PipelineResult permits PipelineResult.Success, PipelineResult.PartialSuccess,
            PipelineResult.Failure, PipelineResult.Rollback {

        String getPipelineId();
        boolean isSuccess();
        java.time.Instant getCompletedAt();
        Duration getTotalDuration();
        List<StageResult> getStageResults();

        record Success(
            String pipelineId,
            Map<String, Object> output,
            java.time.Instant completedAt,
            Duration totalDuration,
            List<StageResult> stageResults,
            String commitHash
        ) implements PipelineResult {
            @Override public boolean isSuccess() { return true; }
            @Override public String getPipelineId() { return pipelineId; }
            @Override public java.time.Instant getCompletedAt() { return completedAt; }
            @Override public Duration getTotalDuration() { return totalDuration; }
            @Override public List<StageResult> getStageResults() { return stageResults; }
        }

        record PartialSuccess(
            String pipelineId,
            Map<String, Object> output,
            List<ValidationError> remainingErrors,
            java.time.Instant completedAt,
            Duration totalDuration,
            List<StageResult> stageResults
        ) implements PipelineResult {
            @Override public boolean isSuccess() { return true; }
            @Override public String getPipelineId() { return pipelineId; }
            @Override public java.time.Instant getCompletedAt() { return completedAt; }
            @Override public Duration getTotalDuration() { return totalDuration; }
            @Override public List<StageResult> getStageResults() { return stageResults; }
        }

        record Failure(
            String pipelineId,
            String failedStage,
            String errorCode,
            String errorMessage,
            java.time.Instant completedAt,
            Duration totalDuration,
            List<StageResult> stageResults,
            boolean rollbackTriggered
        ) implements PipelineResult {
            @Override public boolean isSuccess() { return false; }
            @Override public String getPipelineId() { return pipelineId; }
            @Override public java.time.Instant getCompletedAt() { return completedAt; }
            @Override public Duration getTotalDuration() { return totalDuration; }
            @Override public List<StageResult> getStageResults() { return stageResults; }
        }

        record Rollback(
            String pipelineId,
            int rolledBackToStage,
            String reason,
            java.time.Instant completedAt,
            Duration totalDuration,
            List<StageResult> stageResults
        ) implements PipelineResult {
            @Override public boolean isSuccess() { return false; }
            @Override public String getPipelineId() { return pipelineId; }
            @Override public java.time.Instant getCompletedAt() { return completedAt; }
            @Override public Duration getTotalDuration() { return totalDuration; }
            @Override public List<StageResult> getStageResults() { return stageResults; }
        }
    }

    /**
     * Stage execution result
     */
    record StageResult(
        String stageName,
        StageStatus status,
        java.time.Instant startedAt,
        java.time.Instant completedAt,
        Duration duration,
        Object output,
        List<ValidationError> errors,
        Map<String, String> metadata
    ) {
        public enum StageStatus {
            PENDING, RUNNING, SUCCESS, FAILED, SKIPPED, ROLLED_BACK
        }
    }

    /**
     * Pipeline validation result
     */
    record ValidationResult(
        boolean isValid,
        String schemaValidationResult,
        String businessRuleCheckResult,
        String staticAnalysisResult,
        List<ValidationError> errors,
        Duration duration
    ) {}

    /**
     * Validation error
     */
    record ValidationError(
        String stage,
        String field,
        String code,
        String message,
        Severity severity,
        String suggestion
    ) {
        public enum Severity { INFO, WARN, ERROR, CRITICAL }
    }

    /**
     * Rollback result
     */
    record RollbackResult(
        boolean success,
        String pipelineId,
        int rolledBackToStage,
        String message,
        java.time.Instant completedAt
    ) {}

    /**
     * Pipeline status
     */
    record PipelineStatus(
        String pipelineId,
        PipelineState state,
        int currentStage,
        int totalStages,
        double progressPercentage,
        java.time.Instant startedAt,
        java.time.Duration estimatedRemaining
    ) {
        public enum PipelineState {
            PENDING, RUNNING, PAUSED, COMPLETED, FAILED, ROLLING_BACK, ROLLED_BACK
        }
    }

    /**
     * Pipeline metrics
     */
    record PipelineMetrics(
        String pipelineId,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        Duration llmLatency,
        Duration validationLatency,
        Duration generationLatency,
        Duration staticAnalysisLatency,
        Duration testGenerationLatency,
        double costEstimate
    ) {}
}