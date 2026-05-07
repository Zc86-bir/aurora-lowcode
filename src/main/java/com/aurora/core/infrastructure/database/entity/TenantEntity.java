package com.aurora.core.infrastructure.database.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for the {@code tenant} table.
 *
 * <p>Represents a tenant in the multi-tenant SaaS platform.
 * Supports schema-level, RLS-level, and database-level isolation modes.
 */
@Entity
@Table(name = "tenant")
public class TenantEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_code", nullable = false, unique = true, length = 64)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 128)
    private String tenantName;

    @Column(nullable = false, length = 32)
    private String tier;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "schema_name", length = 64)
    private String schemaName;

    @Column(name = "isolation_mode", nullable = false, length = 16)
    private String isolationMode;

    @Column(name = "max_metadata", nullable = false)
    private int maxMetadata;

    @Column(name = "max_skills", nullable = false)
    private int maxSkills;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "session_timeout", nullable = false)
    private int sessionTimeout;

    @Column(name = "quota_metadata", nullable = false)
    private int quotaMetadata;

    @Column(name = "quota_skills", nullable = false)
    private int quotaSkills;

    @Column(name = "quota_users", nullable = false)
    private int quotaUsers;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int versionLock;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getIsolationMode() { return isolationMode; }
    public void setIsolationMode(String isolationMode) { this.isolationMode = isolationMode; }

    public int getMaxMetadata() { return maxMetadata; }
    public void setMaxMetadata(int maxMetadata) { this.maxMetadata = maxMetadata; }

    public int getMaxSkills() { return maxSkills; }
    public void setMaxSkills(int maxSkills) { this.maxSkills = maxSkills; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public int getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(int sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public int getQuotaMetadata() { return quotaMetadata; }
    public void setQuotaMetadata(int quotaMetadata) { this.quotaMetadata = quotaMetadata; }

    public int getQuotaSkills() { return quotaSkills; }
    public void setQuotaSkills(int quotaSkills) { this.quotaSkills = quotaSkills; }

    public int getQuotaUsers() { return quotaUsers; }
    public void setQuotaUsers(int quotaUsers) { this.quotaUsers = quotaUsers; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getVersionLock() { return versionLock; }
    public void setVersionLock(int versionLock) { this.versionLock = versionLock; }
}
