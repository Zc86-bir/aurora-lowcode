package com.aurora.core.contract;

import com.aurora.core.architecture.Specification;
import com.aurora.core.architecture.Repository.PageResult;
import com.aurora.core.architecture.Repository.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Metadata Repository Interface
 *
 * Provides persistence operations for metadata entities.
 * All metadata is versioned and supports GitOps-style management.
 */
public interface MetadataRepository {

    /**
     * Find metadata by ID.
     */
    Optional<MetadataAggregate> findById(MetadataId id);

    /**
     * Find metadata by type.
     */
    List<MetadataAggregate> findByType(MetadataType type);

    /**
     * Find metadata by tenant.
     */
    List<MetadataAggregate> findByTenant(UUID tenantId);

    /**
     * Find active (non-deleted) metadata.
     */
    List<MetadataAggregate> findActive();

    /**
     * Find metadata by version.
     */
    Optional<MetadataAggregate> findByIdAndVersion(MetadataId id, int version);

    /**
     * Get all versions of a metadata.
     */
    List<MetadataVersion> getVersions(MetadataId id);

    /**
     * Get the latest version number.
     */
    int getLatestVersion(MetadataId id);

    /**
     * Compute diff between two versions.
     */
    MetadataDiff computeDiff(MetadataId id, int fromVersion, int toVersion);

    /**
     * Rollback to a specific version.
     */
    MetadataAggregate rollback(MetadataId id, int targetVersion);

    /**
     * Find by name (unique within tenant).
     */
    Optional<MetadataAggregate> findByTenantAndName(UUID tenantId, String name);

    /**
     * Check if name exists in tenant.
     */
    boolean existsByTenantAndName(UUID tenantId, String name);

    /**
     * Save metadata.
     */
    MetadataAggregate save(MetadataAggregate aggregate);

    /**
     * Save all metadata in batch.
     */
    List<MetadataAggregate> saveAll(List<MetadataAggregate> aggregates);

    /**
     * Delete metadata by ID.
     */
    void deleteById(MetadataId id);

    /**
     * Bulk update metadata status.
     */
    void updateStatus(List<MetadataId> ids, MetadataStatus status);

    /**
     * Find modified after timestamp (for sync).
     */
    List<MetadataAggregate> findModifiedAfter(java.time.Instant timestamp);

    /**
     * Find with pagination.
     */
    PageResult<MetadataAggregate> findPaged(PageRequest pageRequest);

    /**
     * Find by tenant with pagination.
     */
    PageResult<MetadataAggregate> findPagedByTenant(UUID tenantId, PageRequest pageRequest);

    /**
     * Count by tenant.
     */
    long countByTenant(UUID tenantId);

    // Value types

    /**
     * Metadata aggregate identifier
     */
    record MetadataId(UUID value) {
        public static MetadataId generate() {
            return new MetadataId(UUID.randomUUID());
        }

        public static MetadataId of(String value) {
            return new MetadataId(UUID.fromString(value));
        }
    }

    /**
     * Metadata type enumeration
     */
    enum MetadataType {
        FORM, REPORT, WORKFLOW, DASHBOARD,
        CHART, TABLE, API, CONFIG,
        PERMISSION, TEMPLATE
    }

    /**
     * Metadata status enumeration
     */
    enum MetadataStatus {
        DRAFT, PUBLISHED, DEPRECATED, ARCHIVED
    }

    /**
     * Metadata version record
     */
    record MetadataVersion(
        int versionNumber,
        java.time.Instant createdAt,
        String createdBy,
        String changeDescription,
        String checksum
    ) {}

    /**
     * Metadata diff record
     */
    record MetadataDiff(
        int fromVersion,
        int toVersion,
        List<DiffEntry> entries,
        java.time.Instant computedAt
    ) {
        record DiffEntry(
            String path,
            DiffType type,
            Object oldValue,
            Object newValue
        ) {}

        enum DiffType { ADD, REMOVE, MODIFY, MOVE }
    }

    /**
     * Metadata aggregate sealed hierarchy.
     * Each subtype extends AggregateRoot for DDD event sourcing support.
     */
    sealed interface MetadataAggregate permits MetadataAggregate.FormMetadata,
            MetadataAggregate.ReportMetadata, MetadataAggregate.WorkflowMetadata,
            MetadataAggregate.DashboardMetadata, MetadataAggregate.ChartMetadata,
            MetadataAggregate.TableMetadata, MetadataAggregate.ApiMetadata,
            MetadataAggregate.ConfigMetadata, MetadataAggregate.PermissionMetadata,
            MetadataAggregate.TemplateMetadata {

        MetadataId getId();
        UUID getTenantId();
        MetadataType getType();
        String getName();
        int getVersion();
        MetadataStatus getStatus();
        java.time.Instant getCreatedAt();
        java.time.Instant getUpdatedAt();
        String getCreatedBy();
        String getUpdatedBy();
        String getChecksum();

        record FormMetadata(MetadataId id, UUID tenantId, String name, int version,
                           MetadataStatus status, java.time.Instant createdAt,
                           java.time.Instant updatedAt, String createdBy, String updatedBy,
                           String checksum, Object schema, Object layout) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.FORM; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record ReportMetadata(MetadataId id, UUID tenantId, String name, int version,
                             MetadataStatus status, java.time.Instant createdAt,
                             java.time.Instant updatedAt, String createdBy, String updatedBy,
                             String checksum, Object dataSource, Object columns) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.REPORT; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record WorkflowMetadata(MetadataId id, UUID tenantId, String name, int version,
                               MetadataStatus status, java.time.Instant createdAt,
                               java.time.Instant updatedAt, String createdBy, String updatedBy,
                               String checksum, Object bpmnDefinition, Object permissions) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.WORKFLOW; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record DashboardMetadata(MetadataId id, UUID tenantId, String name, int version,
                                MetadataStatus status, java.time.Instant createdAt,
                                java.time.Instant updatedAt, String createdBy, String updatedBy,
                                String checksum, Object components, Object layout) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.DASHBOARD; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record ChartMetadata(MetadataId id, UUID tenantId, String name, int version,
                            MetadataStatus status, java.time.Instant createdAt,
                            java.time.Instant updatedAt, String createdBy, String updatedBy,
                            String checksum, Object chartConfig, Object dataBinding) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.CHART; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record TableMetadata(MetadataId id, UUID tenantId, String name, int version,
                            MetadataStatus status, java.time.Instant createdAt,
                            java.time.Instant updatedAt, String createdBy, String updatedBy,
                            String checksum, Object columns, Object query) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.TABLE; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record ApiMetadata(MetadataId id, UUID tenantId, String name, int version,
                          MetadataStatus status, java.time.Instant createdAt,
                          java.time.Instant updatedAt, String createdBy, String updatedBy,
                          String checksum, Object endpoint, Object swagger) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.API; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record ConfigMetadata(MetadataId id, UUID tenantId, String name, int version,
                            MetadataStatus status, java.time.Instant createdAt,
                            java.time.Instant updatedAt, String createdBy, String updatedBy,
                            String checksum, Object properties) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.CONFIG; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record PermissionMetadata(MetadataId id, UUID tenantId, String name, int version,
                                  MetadataStatus status, java.time.Instant createdAt,
                                  java.time.Instant updatedAt, String createdBy, String updatedBy,
                                  String checksum, Object rules) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.PERMISSION; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }

        record TemplateMetadata(MetadataId id, UUID tenantId, String name, int version,
                               MetadataStatus status, java.time.Instant createdAt,
                               java.time.Instant updatedAt, String createdBy, String updatedBy,
                               String checksum, Object templateContent) implements MetadataAggregate {
            @Override public MetadataId getId() { return id; }
            @Override public UUID getTenantId() { return tenantId; }
            @Override public MetadataType getType() { return MetadataType.TEMPLATE; }
            @Override public String getName() { return name; }
            @Override public int getVersion() { return version; }
            @Override public MetadataStatus getStatus() { return status; }
            @Override public java.time.Instant getCreatedAt() { return createdAt; }
            @Override public java.time.Instant getUpdatedAt() { return updatedAt; }
            @Override public String getCreatedBy() { return createdBy; }
            @Override public String getUpdatedBy() { return updatedBy; }
            @Override public String getChecksum() { return checksum; }
        }
    }
}
