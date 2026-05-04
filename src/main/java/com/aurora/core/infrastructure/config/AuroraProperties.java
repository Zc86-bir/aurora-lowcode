package com.aurora.core.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Centralized configuration properties for Aurora platform.
 * Replaces all @Value annotations with type-safe, validated configuration.
 *
 * Usage: inject via @ConfigurationPropertiesScan or @EnableConfigurationProperties.
 * Environment variable override: AURORA_TENANT_DEFAULT_TIER, AURORA_SECURITY_JWT_SECRET, etc.
 */
@Validated
@ConfigurationProperties(prefix = "aurora")
public record AuroraProperties(
    @NotNull Tenant tenant,
    @NotNull Security security,
    @NotNull Skill skill,
    @NotNull AiPipeline aiPipeline,
    @NotNull Metadata metadata,
    @NotNull CodeGenerator codeGenerator
) {

    public record Tenant(
        @NotBlank String defaultTier,
        @Min(1) int maxMetadataPerTenant,
        @Min(1) int maxSkillsPerTenant,
        Duration sessionTimeout
    ) {}

    public record Security(
        @NotBlank String jwtSecret,
        @Min(900) Duration jwtExpiration,
        @Min(1) int maxLoginAttempts,
        Duration lockoutDuration,
        @NotNull Cors cors,
        @NotNull RateLimit rateLimit
    ) {
        public record Cors(
            @NotNull List<String> allowedOrigins,
            @NotNull List<String> allowedMethods,
            Duration maxAge
        ) {}

        public record RateLimit(
            @Min(1) int requestsPerMinute,
            @Min(1) int requestsPerHour,
            boolean enabled
        ) {}
    }

    public record Skill(
        @NotNull Execution execution,
        @NotNull Sandbox sandbox,
        @NotNull Fallback fallback
    ) {
        public record Execution(
            @Min(1) Duration defaultTimeout,
            @Min(1) int maxConcurrentExecutions,
            boolean retryOnFailure
        ) {}

        public record Sandbox(
            boolean enabled,
            @Min(1) Duration timeout,
            @Min(16) int maxMemoryMb,
            boolean networkIsolation
        ) {}

        public record Fallback(
            @NotNull Strategy defaultStrategy,
            int maxRetries,
            Duration retryDelay
        ) {
            public enum Strategy { CACHE, DEFAULT_VALUE, GRACEFUL, RETRY, REDIRECT }
        }
    }

    public record AiPipeline(
        @NotNull Validation validation,
        @NotNull Output output
    ) {
        public record Validation(
            boolean schemaValidationEnabled,
            boolean businessRuleCheckEnabled,
            boolean staticAnalysisEnabled,
            boolean testGenerationEnabled
        ) {}

        public record Output(
            @NotBlank String basePath,
            boolean createGitCommit,
            boolean enableRollback,
            @Min(1) int maxFilesPerGeneration
        ) {}
    }

    public record Metadata(
        @NotNull Versioning versioning,
        @NotNull HotReload hotReload,
        @NotNull Storage storage
    ) {
        public record Versioning(
            boolean enabled,
            int maxVersionsPerMetadata,
            Duration retentionPeriod
        ) {}

        public record HotReload(
            boolean enabled,
            @Min(100) int maxCacheSize,
            Duration checkInterval
        ) {}

        public record Storage(
            @NotBlank String type,
            @NotNull Map<String, String> connectionProperties
        ) {}
    }

    public record CodeGenerator(
        @NotNull Java java,
        @NotNull Vue vue,
        @NotNull Sql sql
    ) {
        public record Java(
            @NotBlank String basePackage,
            @NotNull List<String> excludedPatterns,
            boolean enableSwagger,
            boolean enableComments
        ) {}

        public record Vue(
            @NotBlank String componentsPath,
            @NotBlank String typesPath,
            boolean enableTypeScript,
            boolean enableCompositionApi
        ) {}

        public record Sql(
            @NotBlank String migrationPath,
            @NotBlank String dialect,
            boolean enableIndexes,
            boolean enableTriggers
        ) {}
    }
}