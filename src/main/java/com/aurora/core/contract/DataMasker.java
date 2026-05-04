package com.aurora.core.contract;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * Data Masker Interface
 *
 * Provides data masking and anonymization for sensitive information.
 * Supports field-level, row-level, and custom masking strategies.
 */
public interface DataMasker {

    /**
     * Mask a single field value.
     */
    String maskField(String fieldName, String value, MaskingStrategy strategy);

    /**
     * Mask multiple fields in a record.
     */
    Map<String, Object> maskRecord(Map<String, Object> record, MaskingConfig config);

    /**
     * Mask a collection of records.
     */
    List<Map<String, Object>> maskRecords(
        List<Map<String, Object>> records,
        MaskingConfig config
    );

    /**
     * Mask data based on user permission level.
     */
    Map<String, Object> maskForUser(
        Map<String, Object> record,
        UUID userId,
        UUID tenantId,
        String resource
    );

    /**
     * Get masking strategy for a field.
     */
    MaskingStrategy getStrategy(String fieldName, String resource);

    /**
     * Register a custom masking strategy.
     */
    void registerStrategy(String strategyName, CustomMaskingStrategy strategy);

    /**
     * Validate masking configuration.
     */
    MaskingValidationResult validateConfig(MaskingConfig config);

    /**
     * Unmask data for authorized users (audit trail required).
     */
    UnmaskResult unmaskForAudit(
        Map<String, Object> maskedRecord,
        UUID userId,
        UUID tenantId,
        String auditReason
    );

    // Value types

    /**
     * Masking strategy enum
     */
    enum MaskingStrategy {
        FULL_MASK,          // Complete replacement: "***"
        PARTIAL_MASK,       // Keep first/last chars: "J***n"
        HASH,               // Deterministic hash for joins
        RANDOMIZE,          // Random replacement
        TOKENIZE,           // Replace with token, store mapping
        REDACT,             // Remove field entirely
        NULLIFY,            // Replace with null
        GENERALIZE,         // Reduce precision: "25-30" for age
        PSEUDONYMIZE,       // Replace with fake but consistent value
        CUSTOM              // User-defined strategy
    }

    /**
     * Masking configuration record
     */
    record MaskingConfig(
        UUID tenantId,
        String resource,
        Set<String> sensitiveFields,
        Map<String, MaskingStrategy> fieldStrategies,
        Map<String, MaskingRule> customRules,
        boolean preserveNulls,
        boolean auditUnmasking,
        int retentionDays
    ) {
        public record MaskingRule(
            String fieldName,
            MaskingStrategy strategy,
            String pattern,
            String replacement,
            int preserveChars,
            boolean reversible,
            Map<String, Object> parameters
        ) {}
    }

    /**
     * Custom masking strategy interface
     */
    interface CustomMaskingStrategy {
        String mask(String value, Map<String, Object> parameters);
        String unmask(String maskedValue, Map<String, Object> parameters);
        boolean isReversible();
    }

    /**
     * Masking validation result
     */
    record MaskingValidationResult(
        boolean valid,
        List<MaskingValidationError> errors,
        Set<String> coveredFields,
        Set<String> uncoveredSensitiveFields
    ) {
        public record MaskingValidationError(
            String fieldName,
            String errorCode,
            String message,
            String suggestion
        ) {}
    }

    /**
     * Field-level masking rule interface
     */
    interface FieldMaskingRule {
        String mask(String value);
        boolean isReversible();
        String description();
        MaskingStrategy strategy();
    }

    /**
     * Row-level filter interface
     */
    interface RowLevelFilter {
        boolean test(UUID userId, Map<String, Object> record);
        String description();
    }

    /**
     * Field masking policy
     */
    record FieldMaskingPolicy(
        String fieldName,
        MaskingStrategy strategy,
        boolean isConfigured,
        String description
    ) {}

    /**
     * Unmask result for audit purposes
     */
    record UnmaskResult(
        boolean success,
        Map<String, Object> unmaskedRecord,
        UUID auditId,
        String auditReason,
        java.time.Instant unmaskedAt,
        String deniedReason
    ) {}
}