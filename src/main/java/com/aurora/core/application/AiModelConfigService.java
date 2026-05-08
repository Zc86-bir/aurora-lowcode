package com.aurora.core.application;

import com.aurora.core.infrastructure.database.entity.AiModelConfigEntity;
import com.aurora.core.infrastructure.database.repository.AiModelConfigRepositoryJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AI Model Configuration Service — manages LLM provider configs per tenant.
 *
 * <p>Operations:
 * <ul>
 *   <li>CRUD for model configurations</li>
 *   <li>Default model switching (only one default per tenant)</li>
 *   <li>Enable/disable status management</li>
 *   <li>Test connection to verify provider reachability</li>
 * </ul>
 */
@Service
public class AiModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiModelConfigService.class);

    private final AiModelConfigRepositoryJpa repository;

    public AiModelConfigService(AiModelConfigRepositoryJpa repository) {
        this.repository = repository;
    }

    public List<AiModelConfigEntity> listByTenant(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public Optional<AiModelConfigEntity> getById(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id);
    }

    public Optional<AiModelConfigEntity> getDefaultConfig(UUID tenantId) {
        return repository.findByTenantIdAndIsDefaultTrue(tenantId);
    }

    @Transactional
    public AiModelConfigEntity create(UUID tenantId, String createdBy, String modelId,
                                       String apiKey, String requestUrl, String displayName,
                                       String provider, boolean isDefault) {
        AiModelConfigEntity entity = new AiModelConfigEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setModelId(modelId);
        entity.setApiKeyEncrypted(apiKey != null ? encrypt(apiKey) : null);
        entity.setRequestUrl(requestUrl);
        entity.setDisplayName(displayName != null ? displayName : modelId);
        entity.setProvider(provider != null ? provider : inferProvider(modelId));
        entity.setStatus("ENABLED");
        entity.setCreatedBy(createdBy);

        if (isDefault) {
            repository.clearDefaultByTenantId(tenantId);
            entity.setDefault(true);
        }

        return repository.save(entity);
    }

    @Transactional
    public Optional<AiModelConfigEntity> update(UUID tenantId, UUID id, String updatedBy,
                                                  String modelId, String apiKey,
                                                  String requestUrl, String displayName) {
        return repository.findByTenantIdAndId(tenantId, id).map(entity -> {
            if (modelId != null) entity.setModelId(modelId);
            if (apiKey != null && !apiKey.isEmpty()) {
                entity.setApiKeyEncrypted(encrypt(apiKey));
            }
            if (requestUrl != null) entity.setRequestUrl(requestUrl);
            if (displayName != null) entity.setDisplayName(displayName);
            entity.setUpdatedBy(updatedBy);
            return repository.save(entity);
        });
    }

    @Transactional
    public boolean delete(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).map(entity -> {
            repository.delete(entity);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<AiModelConfigEntity> setDefault(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).map(entity -> {
            repository.clearDefaultByTenantId(tenantId);
            entity.setDefault(true);
            return repository.save(entity);
        });
    }

    @Transactional
    public Optional<AiModelConfigEntity> toggleStatus(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).map(entity -> {
            String newStatus = "ENABLED".equals(entity.getStatus()) ? "DISABLED" : "ENABLED";
            entity.setStatus(newStatus);
            return repository.save(entity);
        });
    }

    /**
     * Test connection to the provider endpoint.
     * Sends a lightweight HEAD or GET request to verify reachability.
     */
    public TestConnectionResult testConnection(String requestUrl, String apiKey) {
        if (requestUrl == null || requestUrl.isBlank()) {
            return new TestConnectionResult(false, "REQUEST_URL_REQUIRED", 0);
        }

        Instant start = Instant.now();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + (apiKey != null ? apiKey : "test"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Duration elapsed = Duration.between(start, Instant.now());

            boolean success = response.statusCode() < 500;
            String message = success
                    ? "Connection successful (HTTP " + response.statusCode() + ")"
                    : "Provider returned HTTP " + response.statusCode();

            return new TestConnectionResult(success, message, elapsed.toMillis());

        } catch (java.net.http.HttpTimeoutException e) {
            Duration elapsed = Duration.between(start, Instant.now());
            return new TestConnectionResult(false, "TIMEOUT", elapsed.toMillis());
        } catch (Exception e) {
            Duration elapsed = Duration.between(start, Instant.now());
            return new TestConnectionResult(false, "CONNECTION_FAILED: " + e.getMessage(), elapsed.toMillis());
        }
    }

    public record TestConnectionResult(boolean success, String message, long latencyMs) {}

    public Map<String, Object> getStats(UUID tenantId) {
        List<AiModelConfigEntity> configs = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        long enabled = configs.stream().filter(c -> "ENABLED".equals(c.getStatus())).count();
        long disabled = configs.stream().filter(c -> "DISABLED".equals(c.getStatus())).count();
        boolean hasDefault = configs.stream().anyMatch(AiModelConfigEntity::isDefault);

        return Map.of(
                "total", configs.size(),
                "enabled", enabled,
                "disabled", disabled,
                "hasDefault", hasDefault
        );
    }

    // Internal

    private String encrypt(String rawKey) {
        // TODO: integrate with proper encryption service (AES-256-GCM)
        // For now, use a simple reversible encoding. In production, replace with
        // a real KMS or vault-backed encryption.
        return java.util.Base64.getEncoder().encodeToString(rawKey.getBytes());
    }

    private String decrypt(String encryptedKey) {
        if (encryptedKey == null) return null;
        return new String(java.util.Base64.getDecoder().decode(encryptedKey));
    }

    private String inferProvider(String modelId) {
        if (modelId == null) return "unknown";
        String lower = modelId.toLowerCase();
        if (lower.contains("claude") || lower.contains("anthropic")) return "anthropic";
        if (lower.contains("gpt") || lower.contains("openai")) return "openai";
        if (lower.contains("gemini") || lower.contains("google")) return "google";
        if (lower.contains("llama") || lower.contains("meta")) return "meta";
        return "custom";
    }
}
