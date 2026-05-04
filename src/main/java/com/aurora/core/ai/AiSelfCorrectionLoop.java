package com.aurora.core.ai;

import com.aurora.core.contract.AIPipeline;
import com.aurora.core.contract.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Self-Correction Loop
 *
 * Implements the defense-in-depth validation pipeline:
 * 1. LLM output → JSON Schema validation (Draft 2020-12)
 * 2. If fails → return error stack to LLM for correction (max 2 rounds)
 * 3. If still fails → trigger Fallback Skill
 *
 * Never trust raw LLM output. Triple validation: Schema + AST + Rules.
 */
public class AiSelfCorrectionLoop {

    private static final Logger log = LoggerFactory.getLogger(AiSelfCorrectionLoop.class);

    private static final int MAX_CORRECTION_ROUNDS = 2;

    private final JsonSchemaValidator schemaValidator;
    private final AstSyntaxFirewall astFirewall;
    private final BusinessRuleEngine ruleEngine;
    private final AuditLogger auditLogger;
    private final SkillTelemetry telemetry;
    private final FallbackStrategy fallbackStrategy;

    public AiSelfCorrectionLoop(JsonSchemaValidator schemaValidator,
                                 AstSyntaxFirewall astFirewall,
                                 BusinessRuleEngine ruleEngine,
                                 AuditLogger auditLogger,
                                 SkillTelemetry telemetry,
                                 FallbackStrategy fallbackStrategy) {
        this.schemaValidator = schemaValidator;
        this.astFirewall = astFirewall;
        this.ruleEngine = ruleEngine;
        this.auditLogger = auditLogger;
        this.telemetry = telemetry;
        this.fallbackStrategy = fallbackStrategy;
    }

    /**
     * Execute self-correction loop with validation pipeline.
     *
     * @param llmOutput Raw LLM generation output
     * @param skillId The skill that produced this output
     * @param schemaJson JSON Schema (Draft 2020-12) for validation
     * @param sourceCode Generated source code (for AST validation)
     * @return CorrectionResult with validated output or fallback
     */
    public CorrectionResult execute(String llmOutput, String skillId,
                                     String schemaJson, String sourceCode) {
        Instant startedAt = Instant.now();
        List<String> errorHistory = new ArrayList<>();
        String currentOutput = llmOutput;
        int round = 0;

        while (round < MAX_CORRECTION_ROUNDS) {
            // Step 1: JSON Schema validation
            SchemaValidationResult schemaResult = schemaValidator.validate(currentOutput, schemaJson);
            if (!schemaResult.valid()) {
                log.warn("Schema validation failed (round {}): {}", round + 1, schemaResult.errors());
                errorHistory.add("Schema error: " + String.join("; ", schemaResult.errors()));
                currentOutput = requestCorrection(currentOutput, errorHistory, skillId);
                round++;
                continue;
            }

            // Step 2: AST syntax firewall (for Java code)
            if (sourceCode != null && !sourceCode.isEmpty()) {
                AstValidationResult astResult = astFirewall.validate(sourceCode);
                if (!astResult.valid()) {
                    log.warn("AST validation failed (round {}): {}", round + 1, astResult.errors());
                    errorHistory.add("AST error: " + String.join("; ", astResult.errors()));
                    currentOutput = requestCorrection(currentOutput, errorHistory, skillId);
                    round++;
                    continue;
                }
            }

            // Step 3: Business rule engine
            RuleValidationResult ruleResult = ruleEngine.validate(currentOutput, skillId);
            if (!ruleResult.valid()) {
                log.warn("Rule validation failed (round {}): {}", round + 1, ruleResult.errors());
                errorHistory.add("Rule error: " + String.join("; ", ruleResult.errors()));
                currentOutput = requestCorrection(currentOutput, errorHistory, skillId);
                round++;
                continue;
            }

            // All validations passed
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetry.recordValidation(skillId, duration, true, round);

            return new CorrectionResult(
                true,
                currentOutput,
                null,
                round,
                errorHistory,
                duration
            );
        }

        // Max rounds exceeded → trigger fallback
        log.error("Self-correction exhausted after {} rounds for skill {}", MAX_CORRECTION_ROUNDS, skillId);
        Duration duration = Duration.between(startedAt, Instant.now());
        telemetry.recordValidation(skillId, duration, false, round);

        FallbackResult fallback = fallbackStrategy.execute(skillId, errorHistory);

        return new CorrectionResult(
            fallback.success(),
            fallback.output(),
            fallback,
            round,
            errorHistory,
            duration
        );
    }

    /**
     * Request LLM to correct its output with error context.
     */
    private String requestCorrection(String previousOutput, List<String> errorHistory, String skillId) {
        // In production: call LLM API with correction prompt
        // For now: return placeholder
        log.info("Requesting correction for skill {}, errors: {}", skillId, errorHistory.getLast());
        return previousOutput;
    }

    // Value types

    public record CorrectionResult(
        boolean valid,
        String output,
        FallbackResult fallback,
        int correctionRounds,
        List<String> errorHistory,
        Duration duration
    ) {}

    public record SchemaValidationResult(
        boolean valid,
        List<String> errors
    ) {
        public static SchemaValidationResult ok() {
            return new SchemaValidationResult(true, List.of());
        }

        public static SchemaValidationResult fail(List<String> errors) {
            return new SchemaValidationResult(false, errors);
        }
    }

    public record AstValidationResult(
        boolean valid,
        List<String> errors
    ) {
        public static AstValidationResult ok() {
            return new AstValidationResult(true, List.of());
        }

        public static AstValidationResult fail(List<String> errors) {
            return new AstValidationResult(false, errors);
        }
    }

    public record RuleValidationResult(
        boolean valid,
        List<String> errors
    ) {
        public static RuleValidationResult ok() {
            return new RuleValidationResult(true, List.of());
        }

        public static RuleValidationResult fail(List<String> errors) {
            return new RuleValidationResult(false, errors);
        }
    }

    public record FallbackResult(
        boolean success,
        String output,
        String fallbackType,
        String reason
    ) {}

    @FunctionalInterface
    public interface FallbackStrategy {
        FallbackResult execute(String skillId, List<String> errorHistory);
    }
}