package com.aurora.core.architecture;

import java.util.List;
import java.time.Instant;

/**
 * Aggregate Root Marker Interface (DDD Tactical Pattern)
 *
 * An Aggregate is a cluster of associated objects that we treat as a unit
 * for purposes of data changes. Each Aggregate has a root and a boundary.
 *
 * @param <ID> The type of the aggregate's identifier
 */
public interface AggregateRoot<ID> extends Entity<ID> {

    /**
     * Get the domain events that have been raised by this aggregate.
     * These events will be dispatched when the aggregate is saved.
     */
    List<DomainEvent> getDomainEvents();

    /**
     * Clear all domain events after they have been dispatched.
     */
    void clearDomainEvents();

    /**
     * Register a domain event to be dispatched when the aggregate is saved.
     */
    void registerEvent(DomainEvent event);

    /**
     * Get the version number for optimistic locking.
     */
    long getVersion();

    /**
     * Get the creation timestamp.
     */
    Instant getCreatedAt();

    /**
     * Get the last update timestamp.
     */
    Instant getUpdatedAt();
}