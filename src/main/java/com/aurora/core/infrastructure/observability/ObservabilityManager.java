package com.aurora.core.infrastructure.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenTelemetry Observability Manager
 *
 * Provides centralized tracing, metrics, and logging correlation for the platform.
 * Supports multi-tenant span propagation and virtual thread context.
 *
 * Metrics tracked:
 * - skill.execution.count (counter)
 * - skill.execution.duration (histogram)
 * - pipeline.stage.count (counter)
 * - pipeline.stage.duration (histogram)
 * - metadata.operation.count (counter)
 * - tenant.request.count (counter)
 */
public class ObservabilityManager {

    private static final String INSTRUMENTATION_NAME = "com.aurora.lowcode";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private final Meter meter;

    // Counters
    private final LongCounter skillExecutionCounter;
    private final LongCounter pipelineStageCounter;
    private final LongCounter metadataOperationCounter;
    private final LongCounter tenantRequestCounter;
    private final LongCounter permissionCheckCounter;
    private final LongCounter auditLogCounter;

    // Histograms
    private final io.opentelemetry.api.metrics.DoubleHistogram skillExecutionDuration;
    private final io.opentelemetry.api.metrics.DoubleHistogram pipelineStageDuration;

    // Attribute keys
    private static final AttributeKey<String> TENANT_ID = AttributeKey.stringKey("tenant.id");
    private static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");
    private static final AttributeKey<String> SKILL_ID = AttributeKey.stringKey("skill.id");
    private static final AttributeKey<String> PIPELINE_ID = AttributeKey.stringKey("pipeline.id");
    private static final AttributeKey<String> STAGE_NAME = AttributeKey.stringKey("stage.name");
    private static final AttributeKey<String> RESOURCE_TYPE = AttributeKey.stringKey("resource.type");
    private static final AttributeKey<String> OPERATION = AttributeKey.stringKey("operation");
    private static final AttributeKey<String> REQUEST_ID = AttributeKey.stringKey("request.id");
    private static final AttributeKey<Boolean> SUCCESS = AttributeKey.booleanKey("success");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final AttributeKey<String> PERMISSION_RESOURCE = AttributeKey.stringKey("permission.resource");
    private static final AttributeKey<String> PERMISSION_ACTION = AttributeKey.stringKey("permission.action");

    // ScopedValue for virtual thread context propagation (Java 25)
    // Replaces ThreadLocal — no thread pinning, automatic cleanup on scope exit
    public static final ScopedValue<UUID> CURRENT_TENANT = ScopedValue.newInstance();
    public static final ScopedValue<String> CURRENT_REQUEST = ScopedValue.newInstance();

    // Active span tracking
    private final ConcurrentHashMap<String, Span> activeSpans = new ConcurrentHashMap<>();

    public ObservabilityManager(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
        this.meter = openTelemetry.getMeter(INSTRUMENTATION_NAME);

        // Initialize counters
        this.skillExecutionCounter = meter.counterBuilder("skill.execution.count")
            .setDescription("Total number of skill executions")
            .setUnit("1")
            .build();

        this.pipelineStageCounter = meter.counterBuilder("pipeline.stage.count")
            .setDescription("Total number of pipeline stage executions")
            .setUnit("1")
            .build();

        this.metadataOperationCounter = meter.counterBuilder("metadata.operation.count")
            .setDescription("Total number of metadata operations")
            .setUnit("1")
            .build();

        this.tenantRequestCounter = meter.counterBuilder("tenant.request.count")
            .setDescription("Total number of tenant requests")
            .setUnit("1")
            .build();

        this.permissionCheckCounter = meter.counterBuilder("permission.check.count")
            .setDescription("Total number of permission checks")
            .setUnit("1")
            .build();

        this.auditLogCounter = meter.counterBuilder("audit.log.count")
            .setDescription("Total number of audit log entries")
            .setUnit("1")
            .build();

        // Initialize histograms
        this.skillExecutionDuration = meter.histogramBuilder("skill.execution.duration")
            .setDescription("Skill execution duration in milliseconds")
            .setUnit("ms")
            .build();

        this.pipelineStageDuration = meter.histogramBuilder("pipeline.stage.duration")
            .setDescription("Pipeline stage execution duration in milliseconds")
            .setUnit("ms")
            .build();
    }

    // ==================== Tracing ====================

    /**
     * Create and start a new span for skill execution.
     */
    public Span startSkillSpan(UUID tenantId, String skillId, String requestId) {
        Span span = tracer.spanBuilder("skill.execute")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(TENANT_ID, tenantId.toString())
            .setAttribute(SKILL_ID, skillId)
            .setAttribute(REQUEST_ID, requestId)
            .startSpan();

        activeSpans.put(requestId, span);
        return span;
    }

    /**
     * Create and start a new span for pipeline execution.
     */
    public Span startPipelineSpan(UUID tenantId, String pipelineId, String requestId) {
        Span span = tracer.spanBuilder("pipeline.execute")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(TENANT_ID, tenantId.toString())
            .setAttribute(PIPELINE_ID, pipelineId)
            .setAttribute(REQUEST_ID, requestId)
            .startSpan();

        activeSpans.put(requestId, span);
        return span;
    }

    /**
     * Create and start a new span for metadata operation.
     */
    public Span startMetadataSpan(UUID tenantId, String resourceType, String operation, String requestId) {
        Span span = tracer.spanBuilder("metadata.operation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(TENANT_ID, tenantId.toString())
            .setAttribute(RESOURCE_TYPE, resourceType)
            .setAttribute(OPERATION, operation)
            .setAttribute(REQUEST_ID, requestId)
            .startSpan();

        activeSpans.put(requestId, span);
        return span;
    }

    /**
     * Create and start a new span for permission check.
     */
    public Span startPermissionSpan(UUID tenantId, String resource, String action, String requestId) {
        Span span = tracer.spanBuilder("permission.check")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(TENANT_ID, tenantId.toString())
            .setAttribute(PERMISSION_RESOURCE, resource)
            .setAttribute(PERMISSION_ACTION, action)
            .setAttribute(REQUEST_ID, requestId)
            .startSpan();

        activeSpans.put(requestId, span);
        return span;
    }

    /**
     * End a span with success status.
     */
    public void endSpanSuccess(String requestId) {
        Span span = activeSpans.remove(requestId);
        if (span != null) {
            span.setStatus(StatusCode.OK);
            span.end();
        }
    }

    /**
     * End a span with error status.
     */
    public void endSpanError(String requestId, Throwable error) {
        Span span = activeSpans.remove(requestId);
        if (span != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
            span.setAttribute(ERROR_TYPE, error.getClass().getSimpleName());
            span.end();
        }
    }

    /**
     * Get the current active span.
     */
    public Span getActiveSpan(String requestId) {
        return activeSpans.get(requestId);
    }

    // ==================== Metrics ====================

    /**
     * Record skill execution metric.
     */
    public void recordSkillExecution(UUID tenantId, String skillId, Duration duration, boolean success) {
        skillExecutionCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString(),
            SKILL_ID, skillId,
            SUCCESS, success
        ));

        skillExecutionDuration.record(duration.toMillis(), Attributes.of(
            TENANT_ID, tenantId.toString(),
            SKILL_ID, skillId
        ));
    }

    /**
     * Record pipeline stage metric.
     */
    public void recordPipelineStage(UUID tenantId, String pipelineId, String stageName,
                                     Duration duration, boolean success) {
        pipelineStageCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString(),
            PIPELINE_ID, pipelineId,
            STAGE_NAME, stageName,
            SUCCESS, success
        ));

        pipelineStageDuration.record(duration.toMillis(), Attributes.of(
            TENANT_ID, tenantId.toString(),
            PIPELINE_ID, pipelineId,
            STAGE_NAME, stageName
        ));
    }

    /**
     * Record metadata operation metric.
     */
    public void recordMetadataOperation(UUID tenantId, String resourceType, String operation) {
        metadataOperationCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString(),
            RESOURCE_TYPE, resourceType,
            OPERATION, operation
        ));
    }

    /**
     * Record tenant request metric.
     */
    public void recordTenantRequest(UUID tenantId) {
        tenantRequestCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString()
        ));
    }

    /**
     * Record permission check metric.
     */
    public void recordPermissionCheck(UUID tenantId, String resource, String action, boolean allowed) {
        permissionCheckCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString(),
            PERMISSION_RESOURCE, resource,
            PERMISSION_ACTION, action,
            SUCCESS, allowed
        ));
    }

    /**
     * Record audit log metric.
     */
    public void recordAuditLog(UUID tenantId, String eventType) {
        auditLogCounter.add(1, Attributes.of(
            TENANT_ID, tenantId.toString(),
            AttributeKey.stringKey("event.type"), eventType
        ));
    }

    // ==================== Virtual Thread Context ====================

    /**
     * Run code with tenant context propagation using ScopedValue (Java 25).
     * No thread pinning — ScopedValue automatically cleans up on scope exit.
     * Compatible with Spring Framework 6.3 virtual thread execution model.
     *
     * Usage:
     *   obsManager.runWithContext(tenantId, requestId, () -> {
     *       // CURRENT_TENANT and CURRENT_REQUEST are available via ScopedValue.get()
     *       return doWork();
     *   });
     */
    public <T> T runWithContext(UUID tenantId, String requestId, java.util.function.Supplier<T> action) {
        return ScopedValue.where(CURRENT_TENANT, tenantId)
            .where(CURRENT_REQUEST, requestId)
            .call(action::get);
    }

    /**
     * Run code with tenant context (void).
     */
    public void runWithContext(UUID tenantId, String requestId, Runnable action) {
        ScopedValue.where(CURRENT_TENANT, tenantId)
            .where(CURRENT_REQUEST, requestId)
            .run(action);
    }

    /**
     * Get current tenant ID from ScopedValue.
     * Returns null if not bound (i.e., not inside a runWithContext scope).
     */
    public UUID getCurrentTenantId() {
        return CURRENT_TENANT.orElse(null);
    }

    /**
     * Get current request ID from ScopedValue.
     * Returns null if not bound.
     */
    public String getCurrentRequestId() {
        return CURRENT_REQUEST.orElse(null);
    }

    // ==================== Health ====================

    /**
     * Get observability health status.
     */
    public ObservabilityHealth health() {
        return new ObservabilityHealth(
            true,
            activeSpans.size(),
            l1CacheSize(),
            CURRENT_TENANT.isBound() ? CURRENT_TENANT.get().toString() : "none"
        );
    }

    private int l1CacheSize() {
        return activeSpans.size();
    }

    /**
     * Observability health status.
     */
    public record ObservabilityHealth(
        boolean openTelemetryConnected,
        int activeSpans,
        int cacheSize,
        String currentTenant
    ) {}
}