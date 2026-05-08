package com.aurora.core.adapter.web;

import com.aurora.core.application.AiModelConfigService;
import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.database.entity.AiModelConfigEntity;
import com.aurora.core.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "AI Model Configuration", description = "Manage LLM provider configs per tenant")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/ai/models")
public class AiModelConfigController {

    private final AiModelConfigService service;
    private final TenantContext tenantContext;
    private final JwtTokenProvider jwtTokenProvider;

    public AiModelConfigController(AiModelConfigService service,
                                    TenantContext tenantContext,
                                    JwtTokenProvider jwtTokenProvider) {
        this.service = service;
        this.tenantContext = tenantContext;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(summary = "List all AI model configs")
    @GetMapping
    public ResponseEntity<?> listModels(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        List<Map<String, Object>> result = service.listByTenant(tenantId).stream()
                .map(this::toResponseMap).toList();
        return ok(result);
    }

    @Operation(summary = "Get AI model config by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getModel(HttpServletRequest request,
                                       @Parameter(description = "Config ID") @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return service.getById(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(entity -> ok(toResponseMap(entity)))
                .orElse(notFound("MODEL_NOT_FOUND"));
    }

    @Operation(summary = "Create AI model config")
    @PostMapping
    public ResponseEntity<?> createModel(HttpServletRequest request,
                                          @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String modelId = (String) body.get("modelId");
        if (modelId == null || modelId.isBlank()) {
            return badRequest("MODEL_ID_REQUIRED");
        }
        String apiKey = (String) body.get("apiKey");
        String requestUrl = (String) body.get("requestUrl");
        String displayName = (String) body.get("displayName");
        String provider = (String) body.get("provider");
        boolean isDefault = Boolean.TRUE.equals(body.get("isDefault"));
        String createdBy = resolveUsername(request);
        var created = service.create(tenantId, createdBy, modelId,
                apiKey, requestUrl, displayName, provider, isDefault);
        return ok(toResponseMap(created));
    }

    @Operation(summary = "Update AI model config")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateModel(HttpServletRequest request,
                                          @PathVariable String id,
                                          @RequestBody Map<String, Object> body) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        String modelId = (String) body.get("modelId");
        String apiKey = (String) body.get("apiKey");
        String requestUrl = (String) body.get("requestUrl");
        String displayName = (String) body.get("displayName");
        String updatedBy = resolveUsername(request);
        return service.update(tenantId, parseUUID(id), updatedBy, modelId, apiKey, requestUrl, displayName)
                .<ResponseEntity<?>>map(entity -> ok(toResponseMap(entity)))
                .orElse(notFound("MODEL_NOT_FOUND"));
    }

    @Operation(summary = "Delete AI model config")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModel(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        if (!service.delete(tenantId, parseUUID(id))) return notFound("MODEL_NOT_FOUND");
        return ok(Map.of("deleted", true));
    }

    @Operation(summary = "Set default model config")
    @PostMapping("/{id}/default")
    public ResponseEntity<?> setDefault(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return service.setDefault(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(entity -> ok(toResponseMap(entity)))
                .orElse(notFound("MODEL_NOT_FOUND"));
    }

    @Operation(summary = "Toggle model config status")
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleStatus(HttpServletRequest request, @PathVariable String id) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return service.toggleStatus(tenantId, parseUUID(id))
                .<ResponseEntity<?>>map(entity -> ok(toResponseMap(entity)))
                .orElse(notFound("MODEL_NOT_FOUND"));
    }

    @Operation(summary = "Test provider connection")
    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@RequestBody Map<String, Object> body) {
        String requestUrl = (String) body.get("requestUrl");
        String apiKey = (String) body.get("apiKey");
        var result = service.testConnection(requestUrl, apiKey);
        return ok(Map.of("success", result.success(), "message", result.message(), "latencyMs", result.latencyMs()));
    }

    @Operation(summary = "Get model config statistics")
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) return unauthorized();
        return ok(service.getStats(tenantId));
    }

    private UUID resolveTenantId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenProvider.extractTenantId(authHeader.substring(7));
        }
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null) {
            try { return UUID.fromString(tenantHeader); } catch (IllegalArgumentException ignored) {}
        }
        return tenantContext.getCurrentTenantId();
    }

    private String resolveUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtTokenProvider.extractUsername(authHeader.substring(7));
        }
        return "system";
    }

    private UUID parseUUID(String id) { return UUID.fromString(id); }

    private ResponseEntity<?> ok(Object data) {
        return ResponseEntity.ok(new AppResponse<>(true, data, null));
    }

    private ResponseEntity<?> notFound(String code) {
        return ResponseEntity.status(404).body(new AppResponse<>(false, null, code));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(new AppResponse<>(false, null, "NOT_AUTHENTICATED"));
    }

    private ResponseEntity<?> badRequest(String code) {
        return ResponseEntity.badRequest().body(new AppResponse<>(false, null, code));
    }

    private Map<String, Object> toResponseMap(AiModelConfigEntity entity) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", entity.getId().toString());
        m.put("tenantId", entity.getTenantId().toString());
        m.put("modelId", entity.getModelId());
        m.put("requestUrl", entity.getRequestUrl() != null ? entity.getRequestUrl() : "");
        m.put("displayName", entity.getDisplayName() != null ? entity.getDisplayName() : entity.getModelId());
        m.put("provider", entity.getProvider() != null ? entity.getProvider() : "custom");
        m.put("status", entity.getStatus());
        m.put("isDefault", entity.isDefault());
        m.put("createdBy", entity.getCreatedBy());
        m.put("createdAt", entity.getCreatedAt().toString());
        m.put("updatedBy", entity.getUpdatedBy() != null ? entity.getUpdatedBy() : "");
        m.put("updatedAt", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : "");
        return m;
    }

    public record AppResponse<T>(boolean success, T data, String error) {}
}
