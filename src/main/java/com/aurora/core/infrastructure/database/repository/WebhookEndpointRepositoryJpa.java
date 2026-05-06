package com.aurora.core.infrastructure.database.repository;

import com.aurora.core.infrastructure.database.entity.WebhookEndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link WebhookEndpointEntity}.
 *
 * <p>Note: {@code findActiveByTenantAndEvent} uses LIKE for comma-separated
 * event matching. The {@code eventType} parameter is derived from
 * {@link com.aurora.core.architecture.DomainEvent#getEventType()} which
 * returns fixed values (CREATED, UPDATED, etc.) — safe from wildcard injection.
 */
@Repository
public interface WebhookEndpointRepositoryJpa extends JpaRepository<WebhookEndpointEntity, UUID> {

    List<WebhookEndpointEntity> findByTenantId(UUID tenantId);

    @Query("SELECT w FROM WebhookEndpointEntity w WHERE w.tenantId = :tenantId AND w.active = true")
    List<WebhookEndpointEntity> findActiveByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT w FROM WebhookEndpointEntity w WHERE w.tenantId = :tenantId AND w.active = true AND w.events LIKE %:eventType%")
    List<WebhookEndpointEntity> findActiveByTenantAndEvent(
            @Param("tenantId") UUID tenantId,
            @Param("eventType") String eventType);
}
