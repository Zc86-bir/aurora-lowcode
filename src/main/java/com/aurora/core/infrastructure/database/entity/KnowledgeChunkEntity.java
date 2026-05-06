package com.aurora.core.infrastructure.database.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_knowledge_chunk")
public class KnowledgeChunkEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "document_id", nullable = false, columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "project_id", length = 128)
    private String projectId;

    @Column(name = "module_id", length = 128)
    private String moduleId;

    @Column(name = "knowledge_scope", nullable = false, length = 16)
    private String knowledgeScope;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "visibility_policy", nullable = false, length = 64)
    private String visibilityPolicy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "semantic_tags", columnDefinition = "jsonb")
    private Map<String, Object> semanticTags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getKnowledgeScope() { return knowledgeScope; }
    public void setKnowledgeScope(String knowledgeScope) { this.knowledgeScope = knowledgeScope; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
    public String getVisibilityPolicy() { return visibilityPolicy; }
    public void setVisibilityPolicy(String visibilityPolicy) { this.visibilityPolicy = visibilityPolicy; }
    public Map<String, Object> getSemanticTags() { return semanticTags; }
    public void setSemanticTags(Map<String, Object> semanticTags) { this.semanticTags = semanticTags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
