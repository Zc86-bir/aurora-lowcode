package com.aurora.core.architecture;

import java.util.List;

/**
 * Value Object Marker Interface (DDD Tactical Pattern)
 *
 * A Value Object is an object that contains attributes but has no conceptual identity.
 * They should be treated as immutable.
 *
 * Two value objects are equal if they have the same attributes.
 */
public interface ValueObject {

    /**
     * Value objects must be immutable.
     * This method should always return true for proper implementation.
     */
    boolean isImmutable();

    /**
     * Validate that the value object's attributes are valid.
     */
    ValidationResult validate();

    /**
     * Create a copy of this value object.
     */
    ValueObject copy();

    /**
     * Validation result record
     */
    record ValidationResult(
        boolean isValid,
        List<ValidationError> errors
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult invalid(List<ValidationError> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validation error record
     */
    record ValidationError(
        String field,
        String code,
        String message
    ) {}
}