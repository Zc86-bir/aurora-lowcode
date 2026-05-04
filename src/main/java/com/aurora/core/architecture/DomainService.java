package com.aurora.core.architecture;

/**
 * Domain Service Marker Interface (DDD Tactical Pattern)
 *
 * A Domain Service encapsulates domain logic that doesn't naturally fit
 * within an entity or value object. Domain services are stateless and
 * operate on domain objects passed to them.
 *
 * Domain services should:
 * - Be stateless
 * - Have an operation as their core responsibility
 * - Work with domain objects (entities, value objects, aggregates)
 * - Have a meaningful domain name
 */
public interface DomainService {

    /**
     * Get the name of this domain service.
     */
    String getServiceName();

    /**
     * Get the description of what this service does.
     */
    String getDescription();
}