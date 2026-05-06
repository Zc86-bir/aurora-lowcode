package com.aurora.core.infrastructure.security;

import com.aurora.core.infrastructure.database.entity.ApiKeyEntity;
import com.aurora.core.infrastructure.database.repository.ApiKeyRepositoryJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API Key Service — generates, hashes, validates, and manages external API keys.
 *
 * <p>Keys are generated as 32-byte cryptographically random values,
 * prefixed with {@code aurora_sk_} for easy identification in logs.
 * Only the SHA-256 hash is stored in the database; the raw key is
 * returned once at creation time.
 *
 * <p>Key format: {@code aurora_sk_<44-char base64>}
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX = "aurora_sk_";
    private static final int KEY_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepositoryJpa repository;

    public ApiKeyService(ApiKeyRepositoryJpa repository) {
        this.repository = repository;
    }

    /**
     * Generate a new API key for a tenant.
     *
     * @param tenantId  the tenant that owns this key
     * @param name      human-readable name for the key
     * @param scopes    comma-separated scope list (e.g. "form:read,form:write")
     * @param expiresAt optional expiration timestamp (null = never expires)
     * @param createdBy who is creating this key
     * @return ApiKeyCreationResult containing the raw key (shown once) and the entity
     */
    @Transactional
    public ApiKeyCreationResult createApiKey(UUID tenantId, String name, String scopes,
                                              Instant expiresAt, String createdBy) {
        byte[] keyBytes = new byte[KEY_BYTES];
        SECURE_RANDOM.nextBytes(keyBytes);

        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String hashedKey = sha256(keyBytes);

        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setHashedKey(hashedKey);
        entity.setName(name);
        entity.setScopes(scopes);
        entity.setStatus("ACTIVE");
        entity.setExpiresAt(expiresAt);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(Instant.now());

        repository.save(entity);
        log.info("API key '{}' created for tenant={}", name, tenantId);

        return new ApiKeyCreationResult(rawKey, entity);
    }

    /**
     * Validate an API key against stored hashes.
     *
     * @param rawKey the raw key string from the X-API-Key header
     * @return the matching ApiKeyEntity if valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<ApiKeyEntity> validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        byte[] keyBytes;
        try {
            String base64Part = rawKey.substring(KEY_PREFIX.length());
            keyBytes = Base64.getUrlDecoder().decode(base64Part);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid API key format (not valid base64)");
            return Optional.empty();
        }

        String hash = sha256(keyBytes);
        Optional<ApiKeyEntity> entityOpt = repository.findByHashedKeyAndStatus(hash, "ACTIVE");

        if (entityOpt.isPresent()) {
            ApiKeyEntity entity = entityOpt.get();
            // Check expiration
            if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
                log.debug("API key '{}' is expired", entity.getName());
                entity.setStatus("EXPIRED");
                repository.save(entity);
                return Optional.empty();
            }

            // Update last used timestamp
            entity.setLastUsedAt(Instant.now());
            repository.save(entity);
        }

        return entityOpt;
    }

    /**
     * List all active API keys for a tenant.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyEntity> listApiKeys(UUID tenantId) {
        return repository.findActiveByTenantId(tenantId);
    }

    /**
     * Revoke an API key (soft delete — set status to REVOKED).
     */
    @Transactional
    public boolean revokeApiKey(UUID keyId, UUID tenantId) {
        Optional<ApiKeyEntity> entityOpt = repository.findById(keyId);
        if (entityOpt.isEmpty()) return false;

        ApiKeyEntity entity = entityOpt.get();
        if (!entity.getTenantId().equals(tenantId)) {
            log.warn("Attempt to revoke API key {} from wrong tenant", keyId);
            return false;
        }

        entity.setStatus("REVOKED");
        repository.save(entity);
        log.info("API key '{}' revoked for tenant={}", entity.getName(), tenantId);
        return true;
    }

    /**
     * Compute SHA-256 hash of raw key bytes as lowercase hex.
     */
    static String sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Result of API key creation — contains the raw key (shown once).
     */
    public record ApiKeyCreationResult(String rawKey, ApiKeyEntity entity) {}
}
