package com.aurora.core.adapter.web;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.database.entity.WebhookEndpointEntity;
import com.aurora.core.infrastructure.database.repository.WebhookEndpointRepositoryJpa;
import com.aurora.core.infrastructure.webhook.UrlValidator;
import com.aurora.core.infrastructure.webhook.WebhookSecretEncryptor;
import com.aurora.core.infrastructure.webhook.WebhookSigner;
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
 * Webhook Endpoint Management Controller — CRUD for webhook endpoints.
 *
 * <p>Tenant administrators can create, list, update, and delete
 * webhook endpoints that receive domain event notifications.
 *
 * <p>The secret is generated server-side and returned at creation time.
 */
@Tag(name = "Webhooks", description = "Webhook endpoint management for event-driven integrations")
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookEndpointRepositoryJpa repository;
    private final TenantContext tenantContext;
    private final WebhookSecretEncryptor encryptor;

    public WebhookController(WebhookEndpointRepositoryJpa repository,
                              TenantContext tenantContext,
                              WebhookSecretEncryptor encryptor) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.encryptor = encryptor;
    }

    @Operation(summary = "Create a new webhook endpoint",
               description = "Creates a webhook endpoint for the current tenant. Secret is generated automatically.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWebhook(@RequestBody CreateWebhookRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (request.url() == null || request.url().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }
        UrlValidator.validate(request.url(), true);

        String secret = WebhookSigner.generateSecret();

        WebhookEndpointEntity entity = new WebhookEndpointEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUrl(request.url());
        entity.setSecret(encryptor.encrypt(secret));
        entity.setEvents(request.events() != null ? request.events() : "");
        entity.setActive(true);
        entity.setDescription(request.description() != null ? request.description() : "");

        String createdBy = tenantContext.getCurrentUserId() != null
                ? tenantContext.getCurrentUserId().toString()
                : "system";
        entity.setCreatedBy(createdBy);

        repository.save(entity);
        log.info("Webhook endpoint created: {} -> {} for tenant={}",
                entity.getId(), entity.getUrl(), tenantId);

        return ResponseEntity.ok(Map.of(
                "id", entity.getId().toString(),
                "url", entity.getUrl(),
                "secret", secret,
                "events", entity.getEvents(),
                "active", entity.isActive(),
                "description", entity.getDescription(),
                "createdAt", entity.getCreatedAt().toString(),
                "warning", "Store the secret securely. It is used to verify webhook payload signatures."
        ));
    }

    @Operation(summary = "List webhook endpoints for current tenant")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listWebhooks() {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        List<WebhookEndpointEntity> endpoints = repository.findByTenantId(tenantId);

        List<Map<String, Object>> result = endpoints.stream()
                .map(e -> Map.<String, Object>ofEntries(
                        Map.entry("id", e.getId().toString()),
                        Map.entry("url", e.getUrl()),
                        Map.entry("events", e.getEvents()),
                        Map.entry("active", e.isActive()),
                        Map.entry("description", e.getDescription()),
                        Map.entry("successCount", e.getSuccessCount()),
                        Map.entry("failureCount", e.getFailureCount()),
                        Map.entry("lastDeliveredAt", e.getLastDeliveredAt() != null
                                ? e.getLastDeliveredAt().toString() : "never"),
                        Map.entry("lastFailureAt", e.getLastFailureAt() != null
                                ? e.getLastFailureAt().toString() : "never"),
                        Map.entry("lastFailureMessage", e.getLastFailureMessage() != null
                                ? e.getLastFailureMessage() : ""),
                        Map.entry("createdAt", e.getCreatedAt().toString())
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Update a webhook endpoint")
    @PutMapping("/{endpointId}")
    public ResponseEntity<Map<String, Object>> updateWebhook(
            @PathVariable UUID endpointId,
            @RequestBody UpdateWebhookRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        var opt = repository.findById(endpointId);
        if (opt.isEmpty() || !opt.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Webhook not found"));
        }

        WebhookEndpointEntity entity = opt.get();
        if (request.url() != null) entity.setUrl(request.url());
        if (request.events() != null) entity.setEvents(request.events());
        if (request.active() != null) entity.setActive(request.active());
        if (request.description() != null) entity.setDescription(request.description());

        repository.save(entity);
        log.info("Webhook endpoint updated: {}", endpointId);

        return ResponseEntity.ok(Map.of(
                "message", "Webhook updated",
                "id", endpointId.toString()
        ));
    }

    @Operation(summary = "Delete a webhook endpoint")
    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Map<String, Object>> deleteWebhook(@PathVariable UUID endpointId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        var opt = repository.findById(endpointId);
        if (opt.isEmpty() || !opt.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Webhook not found"));
        }

        repository.delete(opt.get());
        log.info("Webhook endpoint deleted: {}", endpointId);

        return ResponseEntity.ok(Map.of(
                "message", "Webhook deleted",
                "id", endpointId.toString()
        ));
    }

    @Operation(summary = "Regenerate webhook secret",
               description = "Generates a new secret for an existing webhook endpoint.")
    @PostMapping("/{endpointId}/regenerate-secret")
    public ResponseEntity<Map<String, Object>> regenerateSecret(@PathVariable UUID endpointId) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        var opt = repository.findById(endpointId);
        if (opt.isEmpty() || !opt.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Webhook not found"));
        }

        String newSecret = WebhookSigner.generateSecret();
        WebhookEndpointEntity entity = opt.get();
        entity.setSecret(encryptor.encrypt(newSecret));
        repository.save(entity);

        log.info("Webhook secret regenerated for endpoint: {}", endpointId);

        return ResponseEntity.ok(Map.of(
                "message", "Secret regenerated",
                "id", endpointId.toString(),
                "secret", newSecret,
                "warning", "Store the new secret securely. Previous deliveries will fail with the old secret."
        ));
    }

    public record CreateWebhookRequest(String url, String events, String description) {}
    public record UpdateWebhookRequest(String url, String events, Boolean active, String description) {}
}
