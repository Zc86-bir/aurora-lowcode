package com.aurora.core.architecture;

/**
 * Entity Marker Interface (DDD Tactical Pattern)
 *
 * An Entity is an object defined by its identity, rather than its attributes.
 * Two entities are equal if they have the same identity, even if their
 * attributes differ.
 *
 * @param <ID> The type of the entity's identifier
 */
public interface Entity<ID> {

    /**
     * Get the unique identifier of this entity.
     */
    ID getId();

    /**
     * Check equality based on identity, not attributes.
     */
    boolean equalsById(ID otherId);
}