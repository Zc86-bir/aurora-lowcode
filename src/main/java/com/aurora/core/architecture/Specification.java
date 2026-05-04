package com.aurora.core.architecture;

import java.util.function.Predicate;

/**
 * Specification Pattern Interface (DDD Tactical Pattern)
 *
 * The Specification pattern allows you to encapsulate domain rules
 * in separate classes that can be combined and reused.
 *
 * @param <T> The type of object this specification applies to
 */
public interface Specification<T> {

    /**
     * Check if an object satisfies this specification.
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * Combine this specification with another using AND logic.
     */
    Specification<T> and(Specification<T> other);

    /**
     * Combine this specification with another using OR logic.
     */
    Specification<T> or(Specification<T> other);

    /**
     * Negate this specification.
     */
    Specification<T> not();

    /**
     * Get a description of this specification for logging/debugging.
     */
    String getDescription();

    // Factory methods for common specifications

    /**
     * Create a specification that always returns true.
     */
    static <T> Specification<T> alwaysTrue() {
        return new Specification<>() {
            @Override
            public boolean isSatisfiedBy(T candidate) { return true; }

            @Override
            public Specification<T> and(Specification<T> other) { return other; }

            @Override
            public Specification<T> or(Specification<T> other) { return this; }

            @Override
            public Specification<T> not() { return alwaysFalse(); }

            @Override
            public String getDescription() { return "Always true"; }
        };
    }

    /**
     * Create a specification that always returns false.
     */
    static <T> Specification<T> alwaysFalse() {
        return new Specification<>() {
            @Override
            public boolean isSatisfiedBy(T candidate) { return false; }

            @Override
            public Specification<T> and(Specification<T> other) { return this; }

            @Override
            public Specification<T> or(Specification<T> other) { return other; }

            @Override
            public Specification<T> not() { return alwaysTrue(); }

            @Override
            public String getDescription() { return "Always false"; }
        };
    }

    /**
     * Create a specification from a predicate.
     */
    static <T> Specification<T> of(Predicate<T> predicate, String description) {
        return new Specification<>() {
            @Override
            public boolean isSatisfiedBy(T candidate) { return predicate.test(candidate); }

            @Override
            public Specification<T> and(Specification<T> other) {
                return Specification.of(
                    t -> predicate.test(t) && other.isSatisfiedBy(t),
                    getDescription() + " AND " + other.getDescription()
                );
            }

            @Override
            public Specification<T> or(Specification<T> other) {
                return Specification.of(
                    t -> predicate.test(t) || other.isSatisfiedBy(t),
                    getDescription() + " OR " + other.getDescription()
                );
            }

            @Override
            public Specification<T> not() {
                return Specification.of(
                    t -> !predicate.test(t),
                    "NOT " + getDescription()
                );
            }

            @Override
            public String getDescription() { return description; }
        };
    }
}