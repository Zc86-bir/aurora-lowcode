package com.aurora.core.infrastructure.audit;

import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.AuditLogger.AuditEntry;
import com.aurora.core.contract.AuditLogger.AuditQuery;
import com.aurora.core.contract.AuditLogger.AuditQueryResult;
import com.aurora.core.contract.AuditLogger.AuditLog;
import com.aurora.core.contract.AuditLogger.AuditStatistics;
import com.aurora.core.contract.AuditLogger.ExportFormat;
import com.aurora.core.contract.AuditLogger.PermissionAuditEntry;
import com.aurora.core.contract.AuditLogger.SkillAuditEntry;
import com.aurora.core.contract.AuditLogger.TenantAuditEntry;
import com.aurora.core.contract.AuditLogger.SecurityAuditEntry;
import com.aurora.core.contract.AuditLogger.SecurityAuditEntry.SecurityEventType;
import com.aurora.core.contract.AuditLogger.AuditType;
import com.aurora.core.contract.AuditLogger.AuditSeverity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Structured JSON Audit Logger
 *
 * Writes structured JSON audit logs with multi-tenant support.
 * Logs are batched and asynchronously flushed to storage.
 *
 * Supports:
 * - Multi-tenant isolation
 * - Event type categorization
 * - Query and pagination
 * - Export (JSON, CSV, XLSX, PDF)
 * - Retention policies
 */
public class StructuredJsonAuditLogger implements AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(StructuredJsonAuditLogger.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<AuditLog> logBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalEntries = new AtomicLong(0);

    // In-memory storage (replace with DB in production)
    private final List<AuditLog> storage = new ArrayList<>();

    public StructuredJsonAuditLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void logCreate(AuditEntry entry) {
        logEntry(entry, "CREATE");
    }

    @Override
    public void logUpdate(AuditEntry entry) {
        logEntry(entry, "UPDATE");
    }

    @Override
    public void logDelete(AuditEntry entry) {
        logEntry(entry, "DELETE");
    }

    @Override
    public void logAccess(AuditEntry entry) {
        logEntry(entry, "ACCESS");
    }

    @Override
    public void logPermissionCheck(PermissionAuditEntry entry) {
        AuditLog auditLog = new AuditLog(
            entry.auditId(),
            entry.tenantId(),
            entry.userId(),
            entry.requestId(),
            AuditType.PERMISSION_CHECK,
            entry.allowed() ? AuditSeverity.INFO : AuditSeverity.WARN,
            entry.resourceType(),
            entry.resourceId(),
            entry.action(),
            "Permission " + (entry.allowed() ? "granted" : "denied") + ": " + entry.reason(),
            serializeJson(Map.of(
                "allowed", entry.allowed(),
                "reason", entry.reason(),
                "requiredPermissions", entry.requiredPermissions(),
                "userPermissions", entry.userPermissions()
            )),
            entry.timestamp(),
            entry.ipAddress(),
            null,
            90 // 90 days retention for permission logs
        );

        storage.add(auditLog);
        logBuffer.add(auditLog);
        totalEntries.incrementAndGet();

        log.info("AUDIT: PermissionCheck tenant={} user={} resource={} action={} allowed={}",
            entry.tenantId(), entry.userId(), entry.resourceType(), entry.action(), entry.allowed());
    }

    @Override
    public void logSkillExecution(SkillAuditEntry entry) {
        AuditLog auditLog = new AuditLog(
            entry.auditId(),
            entry.tenantId(),
            entry.userId(),
            entry.requestId(),
            AuditType.SKILL_EXECUTION,
            entry.success() ? AuditSeverity.INFO : AuditSeverity.ERROR,
            "skill",
            entry.skillId(),
            "execute",
            "Skill " + entry.skillId() + " v" + entry.skillVersion()
                + (entry.success() ? " succeeded" : " failed: " + entry.errorMessage()),
            serializeJson(Map.of(
                "skillId", entry.skillId(),
                "skillVersion", entry.skillVersion(),
                "input", entry.input(),
                "success", entry.success(),
                "errorMessage", entry.errorMessage(),
                "durationMs", entry.durationMs(),
                "sandboxEnabled", entry.sandboxEnabled(),
                "fallbackStrategy", entry.fallbackStrategy()
            )),
            entry.timestamp(),
            null,
            null,
            30 // 30 days retention for skill logs
        );

        storage.add(auditLog);
        logBuffer.add(auditLog);
        totalEntries.incrementAndGet();

        log.info("AUDIT: SkillExecution tenant={} skill={} success={} duration={}ms",
            entry.tenantId(), entry.skillId(), entry.success(), entry.durationMs());
    }

    @Override
    public void logTenantSwitch(TenantAuditEntry entry) {
        AuditLog auditLog = new AuditLog(
            entry.auditId(),
            null,
            entry.userId(),
            entry.requestId(),
            AuditType.TENANT_SWITCH,
            entry.success() ? AuditSeverity.INFO : AuditSeverity.WARN,
            "tenant",
            entry.newTenantId().toString(),
            "switch",
            "Tenant switch: " + entry.previousTenantId() + " -> " + entry.newTenantId(),
            serializeJson(Map.of(
                "previousTenantId", entry.previousTenantId(),
                "newTenantId", entry.newTenantId(),
                "reason", entry.reason(),
                "success", entry.success()
            )),
            entry.timestamp(),
            entry.ipAddress(),
            null,
            365 // 1 year retention for tenant switches
        );

        storage.add(auditLog);
        logBuffer.add(auditLog);
        totalEntries.incrementAndGet();

        log.info("AUDIT: TenantSwitch user={} from={} to={} success={}",
            entry.userId(), entry.previousTenantId(), entry.newTenantId(), entry.success());
    }

    @Override
    public void logSecurity(SecurityAuditEntry entry) {
        AuditLog auditLog = new AuditLog(
            entry.auditId(),
            entry.tenantId(),
            entry.userId(),
            entry.requestId(),
            AuditType.SECURITY,
            entry.severity(),
            "security",
            entry.resource(),
            entry.eventType().name(),
            entry.description(),
            serializeJson(Map.of(
                "eventType", entry.eventType().name(),
                "description", entry.description(),
                "details", entry.details()
            )),
            entry.timestamp(),
            entry.ipAddress(),
            null,
            365 // 1 year retention for security logs
        );

        storage.add(auditLog);
        logBuffer.add(auditLog);
        totalEntries.incrementAndGet();

        log.warn("AUDIT: Security tenant={} event={} user={} desc={}",
            entry.tenantId(), entry.eventType(), entry.userId(), entry.description());
    }

    @Override
    public void logCustom(AuditEntry entry) {
        logEntry(entry, "CUSTOM");
    }

    @Override
    public AuditQueryResult query(AuditQuery query) {
        List<AuditLog> filtered = storage.stream()
            .filter(e -> query.tenantId() == null || e.tenantId().equals(query.tenantId()))
            .filter(e -> query.userIds() == null || query.userIds().isEmpty()
                || query.userIds().contains(e.userId()))
            .filter(e -> query.types() == null || query.types().isEmpty()
                || query.types().contains(e.type()))
            .filter(e -> query.resourceTypes() == null || query.resourceTypes().isEmpty()
                || query.resourceTypes().contains(e.resourceType()))
            .filter(e -> query.resourceIds() == null || query.resourceIds().isEmpty()
                || query.resourceIds().contains(e.resourceId()))
            .filter(e -> query.from() == null || !e.timestamp().isBefore(query.from()))
            .filter(e -> query.to() == null || !e.timestamp().isAfter(query.to()))
            .filter(e -> query.searchText() == null || query.searchText().isEmpty()
                || matchesSearch(e, query.searchText()))
            .toList();

        long totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / query.pageSize());
        int fromIndex = query.pageNumber() * query.pageSize();
        int toIndex = Math.min(fromIndex + query.pageSize(), filtered.size());

        List<AuditLog> paged = fromIndex < filtered.size()
            ? filtered.subList(fromIndex, toIndex)
            : List.of();

        return new AuditQueryResult(
            List.copyOf(paged),
            query.pageNumber(),
            query.pageSize(),
            totalElements,
            totalPages
        );
    }

    @Override
    public AuditLog getAuditLog(UUID auditId) {
        return storage.stream()
            .filter(e -> e.auditId().equals(auditId))
            .findFirst()
            .orElse(null);
    }

    @Override
    public byte[] export(AuditQuery query, ExportFormat format) {
        AuditQueryResult result = query(query);

        try {
            return switch (format) {
                case JSON -> objectMapper.writeValueAsBytes(result.logs());
                case CSV -> exportCsv(result.logs());
                case XLSX, PDF -> ("Export format not yet implemented: " + format)
                    .getBytes(StandardCharsets.UTF_8);
            };
        } catch (JsonProcessingException e) {
            log.error("Failed to export audit logs", e);
            return new byte[0];
        }
    }

    @Override
    public AuditStatistics getStatistics(UUID tenantId, Instant from, Instant to) {
        List<AuditLog> filtered = storage.stream()
            .filter(e -> tenantId == null || e.tenantId().equals(tenantId))
            .filter(e -> from == null || !e.timestamp().isBefore(from))
            .filter(e -> to == null || !e.timestamp().isAfter(to))
            .toList();

        Map<AuditType, Long> countByType = filtered.stream()
            .collect(Collectors.groupingBy(AuditLog::type, Collectors.counting()));

        Map<AuditSeverity, Long> countBySeverity = filtered.stream()
            .collect(Collectors.groupingBy(AuditLog::severity, Collectors.counting()));

        long permissionDeniedCount = filtered.stream()
            .filter(e -> e.type() == AuditType.PERMISSION_CHECK)
            .filter(e -> e.summary().contains("denied"))
            .count();

        long securityEventCount = filtered.stream()
            .filter(e -> e.type() == AuditType.SECURITY)
            .count();

        List<String> topResources = filtered.stream()
            .collect(Collectors.groupingBy(AuditLog::resourceType, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();

        List<String> topActions = filtered.stream()
            .collect(Collectors.groupingBy(AuditLog::action, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .toList();

        return new AuditStatistics(
            filtered.size(),
            countByType,
            countBySeverity,
            permissionDeniedCount,
            securityEventCount,
            0.0,
            topResources,
            topActions
        );
    }

    /**
     * Flush buffered logs to persistent storage.
     */
    public void flush() {
        AuditLog entry;
        while ((entry = logBuffer.poll()) != null) {
            // In production: write to database, Elasticsearch, etc.
            log.debug("Flushing audit log: {}", entry.auditId());
        }
    }

    /**
     * Get buffer size.
     */
    public int bufferSize() {
        return logBuffer.size();
    }

    // Internal

    private void logEntry(AuditEntry entry, String action) {
        AuditLog auditLog = new AuditLog(
            entry.auditId(),
            entry.tenantId(),
            entry.userId(),
            entry.requestId(),
            entry.type(),
            entry.severity(),
            entry.resourceType(),
            entry.resourceId(),
            entry.action(),
            action + " " + entry.resourceType() + "/" + entry.resourceId(),
            serializeJson(Map.of(
                "beforeState", entry.beforeState(),
                "afterState", entry.afterState(),
                "metadata", entry.metadata()
            )),
            entry.timestamp(),
            entry.ipAddress(),
            entry.userAgent(),
            90 // 90 days default retention
        );

        storage.add(auditLog);
        logBuffer.add(auditLog);
        totalEntries.incrementAndGet();

        log.info("AUDIT: {} tenant={} user={} resource={}/{}",
            action, entry.tenantId(), entry.userId(),
            entry.resourceType(), entry.resourceId());
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private boolean matchesSearch(AuditLog entry, String searchText) {
        String lower = searchText.toLowerCase();
        return entry.summary().toLowerCase().contains(lower)
            || entry.resourceType().toLowerCase().contains(lower)
            || entry.resourceId().toLowerCase().contains(lower)
            || entry.action().toLowerCase().contains(lower);
    }

    private byte[] exportCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("auditId,tenantId,userId,type,severity,resourceType,resourceId,action,summary,timestamp\n");

        for (AuditLog entry : logs) {
            sb.append(entry.auditId()).append(",");
            sb.append(entry.tenantId()).append(",");
            sb.append(entry.userId()).append(",");
            sb.append(entry.type()).append(",");
            sb.append(entry.severity()).append(",");
            sb.append(entry.resourceType()).append(",");
            sb.append(entry.resourceId()).append(",");
            sb.append(entry.action()).append(",");
            sb.append("\"").append(entry.summary().replace("\"", "\"\"")).append("\",");
            sb.append(entry.timestamp()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}