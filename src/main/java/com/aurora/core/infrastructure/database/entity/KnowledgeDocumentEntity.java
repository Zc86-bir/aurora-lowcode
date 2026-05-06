package com.aurora.core.infrastructure.database.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_knowledge_document")
public class KnowledgeDocumentEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "project_id", length = 128)
    private String projectId;

    @Column(name = "module_id", length = 128)
    private String moduleId;

    @Column(name = "knowledge_scope", nullable = false, length = 16)
    private String knowledgeScope;

    @Column(name = "source_type", nullable = false, length = 16)
    private String sourceType;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "source_uri", length = 2048)
    private String sourceUri;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "visibility_policy", nullable = false, length = 64)
    private String visibilityPolicy;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "failure_message", length = 1024)
    private String failureMessage;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getKnowledgeScope() { return knowledgeScope; }
    public void setKnowledgeScope(String knowledgeScope) { this.knowledgeScope = knowledgeScope; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceUri() { return sourceUri; }
    public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getVisibilityPolicy() { return visibilityPolicy; }
    public void setVisibilityPolicy(String visibilityPolicy) { this.visibilityPolicy = visibilityPolicy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersionLock() { return versionLock; }
    public void setVersionLock(int versionLock) { this.versionLock = versionLock; }
}
