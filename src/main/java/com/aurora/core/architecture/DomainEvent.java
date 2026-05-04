package com.aurora.core.architecture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event Base Interface
 *
 * Domain events represent something that happened in the domain that
 * other parts of the system need to be aware of.
 *
 * All domain events should be immutable and carry all necessary information.
 */
public sealed interface DomainEvent
    permits DomainEvent.Created, DomainEvent.Updated,
            DomainEvent.Deleted, DomainEvent.StatusChanged,
            DomainEvent.Versioned, DomainEvent.ExecutionEvent {

    UUID getEventId();
    String getAggregateType();
    String getAggregateId();
    Instant getOccurredAt();
    String getCausedBy();
    int getEventVersion();
    UUID getTenantId();
    String getEventType();

    record Created(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        Object payload
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "CREATED"; }
    }

    record Updated(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        Object previousState,
        Object newState,
        List<String> changedFields
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "UPDATED"; }
    }

    record Deleted(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        String reason
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "DELETED"; }
    }

    record StatusChanged(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        String previousStatus,
        String newStatus,
        String reason
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "STATUS_CHANGED"; }
    }

    record Versioned(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        int previousVersion,
        int newVersion,
        String changeDescription
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "VERSIONED"; }
    }

    record ExecutionEvent(
        UUID eventId,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String causedBy,
        int eventVersion,
        UUID tenantId,
        String executionType,
        boolean success,
        long durationMs,
        String errorMessage
    ) implements DomainEvent {
        @Override public UUID getEventId() { return eventId; }
        @Override public String getAggregateType() { return aggregateType; }
        @Override public String getAggregateId() { return aggregateId; }
        @Override public Instant getOccurredAt() { return occurredAt; }
        @Override public String getCausedBy() { return causedBy; }
        @Override public int getEventVersion() { return eventVersion; }
        @Override public UUID getTenantId() { return tenantId; }
        @Override public String getEventType() { return "EXECUTION"; }
    }
}