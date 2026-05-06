package com.aurora.core.adapter.web;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.database.entity.ApiKeyEntity;
import com.aurora.core.infrastructure.security.ApiKeyService;
import com.aurora.core.infrastructure.security.ApiKeyService.ApiKeyCreationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API Key Management Controller — CRUD for external API keys.
 *
 * <p>Tenant administrators can create, list, and revoke API keys
 * used by external systems to call Aurora APIs.
 *
 * <p>The raw key is returned ONLY at creation time (in the response).
 * Subsequent listing shows only metadata (name, scopes, status, last used).
 */
@Tag(name = "API Keys", description = "External API key management for tenant administrators")
@RestController
@RequestMapping("/api/v1/apikeys")
public class ApiKeyController {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyController.class);

    private final ApiKeyService apiKeyService;
    private final TenantContext tenantContext;

    public ApiKeyController(ApiKeyService apiKeyService, TenantContext tenantContext) {
        this.apiKeyService = apiKeyService;
        this.tenantContext = tenantContext;
    }

    @Operation(summary = "Create a new API key",
               description = "Generates a new API key for the current tenant. The raw key is returned once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Key created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody CreateApiKeyRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String name = request.name();
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        }

        String scopes = request.scopes() != null ? request.scopes() : "";
        String createdBy = tenantContext.getCurrentUserId() != null
                ? tenantContext.getCurrentUserId().toString()
                : "system";

        ApiKeyCreationResult result = apiKeyService.createApiKey(tenantId, name, scopes, null, createdBy);
        log.info("API key '{}' created for tenant={}", name, tenantId);

        return ResponseEntity.ok(Map.of(
                "id", result.entity().getId().toString(),
                "name", result.entity().getName(),
                "rawKey", result.rawKey(),
                "scopes", result.entity().getScopes(),
                "status", result.entity().getStatus(),
                "createdAt", result.entity().getCreatedAt().toString(),
                "warning", "Store this key securely. It will not be shown again."
        ));
    }

    @Operation(summary = "List API keys for current tenant",
               description = "Returns metadata for all active API keys (raw keys are never returned).")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listApiKeys() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        List<ApiKeyEntity> keys = apiKeyService.listApiKeys(tenantId);

        List<Map<String, Object>> result = keys.stream()
                .map(k -> {
                    String lastUsed = k.getLastUsedAt() != null
                            ? k.getLastUsedAt().toString() : "never";
                    String expires = k.getExpiresAt() != null
                            ? k.getExpiresAt().toString() : "never";
                    return Map.<String, Object>of(
                            "id", k.getId().toString(),
                            "name", k.getName(),
                            "scopes", k.getScopes(),
                            "status", k.getStatus(),
                            "lastUsedAt", lastUsed,
                            "expiresAt", expires,
                            "createdAt", k.getCreatedAt().toString()
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Revoke an API key",
               description = "Soft-deletes the key (sets status to REVOKED). The key can no longer be used.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Key revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Key not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Map<String, Object>> revokeApiKey(@PathVariable UUID keyId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        boolean revoked = apiKeyService.revokeApiKey(keyId, tenantId);
        if (!revoked) {
            return ResponseEntity.status(404).body(Map.of("error", "Key not found"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "API key revoked successfully",
                "id", keyId.toString()
        ));
    }

    /**
     * Request DTO for API key creation.
     */
    public record CreateApiKeyRequest(String name, String scopes) {}
}
