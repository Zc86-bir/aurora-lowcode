package com.aurora.core.adapter.web;

import com.aurora.core.application.supervisor.SupervisorOrchestrator;
import com.aurora.core.application.supervisor.SupervisorRequest;
import com.aurora.core.contract.PermissionChecker;
import com.aurora.core.contract.TenantContext;
import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.CacheProvider;
import com.aurora.core.generator.CodeGenerator;
import com.aurora.core.generator.CodeGenerator.GenerationContext;
import com.aurora.core.generator.CodeGenerator.FieldDefinition;
import com.aurora.core.runtime.FormRuntimeEngine;
import com.aurora.core.runtime.ReportRuntimeEngine;
import com.aurora.core.runtime.WorkflowRuntimeEngine;
import com.aurora.core.runtime.MetadataHotReloadManager;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Dynamic API Gateway Controller
 *
 * Unified REST API for the low-code platform:
 * - Form CRUD operations
 * - Report execution
 * - Workflow management
 * - Metadata hot-reload
 * - Code generation
 *
 * All endpoints are tenant-scoped and permission-checked.
 */
@Tag(name = "Aurora Platform API", description = "Unified REST API for low-code platform operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@Profile("!dev")
public class ApiGatewayController {

    private final FormRuntimeEngine formEngine;
    private final ReportRuntimeEngine reportEngine;
    private final WorkflowRuntimeEngine workflowEngine;
    private final MetadataHotReloadManager hotReloadManager;
    private final CodeGenerator codeGenerator;
    private final PermissionChecker permissionChecker;
    private final TenantContext tenantContext;
    private final CacheProvider cacheProvider;
    private final AuditLogger auditLogger;
    private final SupervisorOrchestrator supervisorOrchestrator;

    public ApiGatewayController(
        FormRuntimeEngine formEngine,
        ReportRuntimeEngine reportEngine,
        WorkflowRuntimeEngine workflowEngine,
        MetadataHotReloadManager hotReloadManager,
        CodeGenerator codeGenerator,
        PermissionChecker permissionChecker,
        TenantContext tenantContext,
        CacheProvider cacheProvider,
        AuditLogger auditLogger,
        SupervisorOrchestrator supervisorOrchestrator
    ) {
        this.formEngine = formEngine;
        this.reportEngine = reportEngine;
        this.workflowEngine = workflowEngine;
        this.hotReloadManager = hotReloadManager;
        this.codeGenerator = codeGenerator;
        this.permissionChecker = permissionChecker;
        this.tenantContext = tenantContext;
        this.cacheProvider = cacheProvider;
        this.auditLogger = auditLogger;
        this.supervisorOrchestrator = supervisorOrchestrator;
    }

    // ==================== Form APIs ====================

    @Operation(summary = "Render form from metadata", description = "Dynamically renders a form based on stored metadata")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Form rendered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Form not found")
    })
    @GetMapping("/forms/{formName}/render")
    public ResponseEntity<?> renderForm(
        @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
        @Parameter(description = "User ID", required = true) @RequestHeader("X-User-Id") UUID userId,
        @Parameter(description = "Form name", required = true) @PathVariable String formName
    ) {
        if (!permissionChecker.hasPermission(userId, tenantId, "form", "read")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("PERMISSION_DENIED"));
        }

        try {
            var rendered = formEngine.renderForm(tenantId, formName);
            return ResponseEntity.ok(ApiResponse.success(rendered));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/forms/{formName}/validate")
    public ResponseEntity<?> validateForm(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String formName,
        @RequestBody Map<String, Object> formData
    ) {
        var result = formEngine.validateFormData(tenantId, formName, formData);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", result.valid(),
            "errors", result.errors()
        )));
    }

    @PostMapping("/forms/{formName}/visibility")
    public ResponseEntity<?> getVisibility(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestBody Map<String, Object> formData,
        @PathVariable String formName
    ) {
        var visibility = formEngine.applyVisibilityRules(tenantId, formName, formData);
        return ResponseEntity.ok(ApiResponse.success(visibility));
    }

    @GetMapping("/forms/{formName}/defaults")
    public ResponseEntity<?> getFormDefaults(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable String formName
    ) {
        var defaults = formEngine.computeDefaults(tenantId, formName);
        return ResponseEntity.ok(ApiResponse.success(defaults));
    }

    // ==================== Report APIs ====================

    @GetMapping("/reports/{reportName}/execute")
    public ResponseEntity<?> executeReport(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String reportName,
        @RequestParam(required = false) Map<String, Object> filters,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int pageSize = Math.min(size, 100); // Cap at 100 to prevent OOM
        int pageNumber = Math.max(0, page);

        var result = reportEngine.executeReport(tenantId, userId, reportName,
            filters != null ? filters : Map.of(), pageNumber, pageSize);

        if (!result.errors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(result.errors().getFirst().message()));
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "data", result.rows(),
            "pagination", Map.of(
                "total", result.totalRows(),
                "page", result.pageNumber(),
                "totalPages", result.totalPages()
            ),
            "executionTime", result.executionTime().toMillis()
        )));
    }

    @GetMapping("/reports/{reportName}/schema")
    public ResponseEntity<?> getReportSchema(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable String reportName
    ) {
        var schema = reportEngine.getReportSchema(tenantId, reportName);
        return ResponseEntity.ok(ApiResponse.success(schema));
    }

    @GetMapping("/reports/{reportName}/export")
    public ResponseEntity<byte[]> exportReport(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String reportName,
        @RequestParam(required = false) Map<String, Object> filters
    ) {
        byte[] csv = reportEngine.exportCSV(tenantId, userId, reportName,
            filters != null ? filters : Map.of());

        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=" + reportName + ".csv")
            .body(csv);
    }

    // ==================== Workflow APIs ====================

    @PostMapping("/workflows/{workflowName}/start")
    public ResponseEntity<?> startWorkflow(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable String workflowName,
        @RequestBody Map<String, Object> inputData
    ) {
        try {
            var instance = workflowEngine.startWorkflow(tenantId, userId, workflowName, inputData);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "instanceId", instance.id(),
                "status", instance.status().name()
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PostMapping("/workflows/{workflowName}/validate")
    public ResponseEntity<?> validateWorkflow(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable String workflowName
    ) {
        var result = workflowEngine.validateWorkflow(tenantId, workflowName);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", result.valid(),
            "errors", result.errors()
        )));
    }

    @PostMapping("/workflows/instances/{instanceId}/cancel")
    public ResponseEntity<?> cancelWorkflow(
        @PathVariable String instanceId
    ) {
        workflowEngine.cancelInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("cancelled", true)));
    }

    // ==================== Metadata Hot-Reload APIs ====================

    @PostMapping("/metadata/{metadataName}/reload")
    public ResponseEntity<?> hotReloadMetadata(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable String metadataName
    ) {
        var result = hotReloadManager.hotReload(tenantId, metadataName);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/metadata/reload-all")
    public ResponseEntity<?> hotReloadAll(
        @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        var result = hotReloadManager.hotReloadAll(tenantId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/metadata/{metadataName}/rollback")
    public ResponseEntity<?> rollbackMetadata(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @PathVariable String metadataName,
        @RequestParam int targetVersion
    ) {
        var result = hotReloadManager.rollback(tenantId, metadataName, targetVersion);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/metadata/stats")
    public ResponseEntity<?> getMetadataStats() {
        return ResponseEntity.ok(ApiResponse.success(hotReloadManager.getStats()));
    }

    // ==================== Code Generation APIs ====================

    @Operation(summary = "Generate CRUD code from metadata", description = "Generates Java Entity/Controller/Service/Repository + Vue SFC + TypeScript + SQL + Test")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Code generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input parameters")
    })
    @PostMapping("/generate/crud")
    public ResponseEntity<?> generateCRUD(
        @Parameter(description = "Tenant ID", required = true) @RequestHeader("X-Tenant-Id") UUID tenantId,
        @Parameter(description = "User ID", required = true) @RequestHeader("X-User-Id") UUID userId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Code generation request with entity name and fields", required = true)
        @RequestBody Map<String, Object> generationRequest
    ) {
        String entityName = (String) generationRequest.get("entityName");
        String tableName = (String) generationRequest.get("tableName");
        String packagePrefix = (String) generationRequest.getOrDefault("packagePrefix", "com.aurora");

        // Validate entity name (prevent path traversal / injection)
        if (entityName == null || !entityName.matches("^[a-zA-Z_$][a-zA-Z0-9_$]{1,63}$")) {
            return ResponseEntity.badRequest().body(error("Invalid entity name. Must be a valid Java identifier"));
        }
        if (tableName != null && !tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$")) {
            return ResponseEntity.badRequest().body(error("Invalid table name"));
        }
        if (packagePrefix != null && !packagePrefix.matches("^[a-zA-Z_][a-zA-Z0-9_.]{1,127}$")) {
            return ResponseEntity.badRequest().body(error("Invalid package prefix"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldsRaw = (List<Map<String, Object>>) generationRequest.get("fields");

        if (entityName == null || fieldsRaw == null) {
            return ResponseEntity.badRequest().body(error("entityName and fields are required"));
        }

        List<FieldDefinition> fields = fieldsRaw.stream()
            .map(f -> new FieldDefinition(
                (String) f.get("name"),
                (String) f.getOrDefault("label", ""),
                (String) f.getOrDefault("javaType", "String"),
                (String) f.get("columnName"),
                (Boolean) f.getOrDefault("required", false),
                (Boolean) f.getOrDefault("primaryKey", false),
                (Integer) f.getOrDefault("maxLength", 0)
            ))
            .toList();

        GenerationContext ctx = new GenerationContext(
            entityName, tableName, packagePrefix, tenantId, userId, fields
        );

        var result = codeGenerator.generateCRUD(ctx);

        if (!result.success()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.errors().getFirst().message()));
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "files", result.files().stream()
                .map(f -> Map.of("path", f.path(), "type", f.type(), "checksum", f.checksum()))
                .toList(),
            "commitHash", result.commitHash(),
            "duration", result.duration().toMillis()
        )));
    }

    // ==================== Supervisor APIs ====================

    @Operation(summary = "Generate composite application", description = "Uses supervisor orchestration to generate a multi-skill app plan")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Composite app generation started/completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or DAG"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied")
    })
    @PostMapping("/generate/app")
    public ResponseEntity<?> generateCompositeApp(
        @RequestHeader("X-Tenant-Id") UUID tenantId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestBody Map<String, Object> body
    ) {
        String prompt = Objects.toString(body.get("prompt"), "").trim();
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(error("PROMPT_REQUIRED"));
        }

        var result = supervisorOrchestrator.execute(new SupervisorRequest(
                tenantId,
                userId,
                prompt,
                UUID.randomUUID().toString()
        ));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        )));
    }

    // Internal

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "error", message);
    }

    public record ApiResponse<T>(
        boolean success,
        T data,
        String error
    ) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, data, null);
        }

        public static ApiResponse<Void> error(String error) {
            return new ApiResponse<>(false, null, error);
        }
    }
}
