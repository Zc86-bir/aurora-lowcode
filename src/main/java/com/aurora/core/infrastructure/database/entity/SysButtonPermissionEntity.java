package com.aurora.core.infrastructure.database.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for the {@code sys_button_permission} table.
 */
@Entity
@Table(name = "sys_button_permission",
       indexes = {
           @Index(name = "idx_sysbtn_tenant", columnList = "tenant_id"),
           @Index(name = "idx_sysbtn_menu", columnList = "menu_id")
       })
public class SysButtonPermissionEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = "menu_id", nullable = false, columnDefinition = "uuid")
    private UUID menuId;

    @Column(name = "button_code", nullable = false, length = 128)
    private String buttonCode;

    @Column(name = "button_name", nullable = false, length = 128)
    private String buttonName;

    @Column(name = "permission_key", nullable = false, length = 128)
    private String permissionKey;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private int versionLock;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "ACTIVE";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getMenuId() { return menuId; }
    public void setMenuId(UUID menuId) { this.menuId = menuId; }

    public String getButtonCode() { return buttonCode; }
    public void setButtonCode(String buttonCode) { this.buttonCode = buttonCode; }

    public String getButtonName() { return buttonName; }
    public void setButtonName(String buttonName) { this.buttonName = buttonName; }

    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getVersionLock() { return versionLock; }
    public void setVersionLock(int versionLock) { this.versionLock = versionLock; }
}
