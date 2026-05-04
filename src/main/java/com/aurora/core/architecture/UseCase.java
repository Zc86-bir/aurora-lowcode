package com.aurora.core.architecture;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Use Case Marker Interface (DDD Application Layer)
 *
 * A Use Case represents a single business operation that the system can perform.
 * Use cases orchestrate domain objects and services to accomplish a specific goal.
 *
 * Use cases should:
 * - Be single-purpose (one operation per use case)
 * - Have input/output DTOs
 * - Not contain business rules (delegate to domain)
 * - Be named as actions (CreateX, UpdateX, DeleteX, QueryX)
 *
 * @param <R> The request/input type
 * @param <T> The response/output type
 */
public interface UseCase<R, T> {

    /**
     * Execute this use case with the given request.
     */
    T execute(R request);

    /**
     * Get the name of this use case.
     */
    String getUseCaseName();

    /**
     * Validate the request before execution.
     */
    ValidationResult validate(R request);

    /**
     * Execute this use case asynchronously.
     */
    CompletableFuture<T> executeAsync(R request);

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