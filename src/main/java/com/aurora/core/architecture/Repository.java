package com.aurora.core.architecture;

import java.util.List;
import java.util.Optional;

/**
 * Repository Marker Interface (DDD Tactical Pattern)
 *
 * A Repository mediates between the domain and data mapping layers,
 * acting like an in-memory domain object collection.
 *
 * Repositories should only work with Aggregate Roots, not with individual entities.
 *
 * @param <T> The type of the aggregate root
 * @param <ID> The type of the aggregate's identifier
 */
public interface Repository<T extends AggregateRoot<ID>, ID> {

    /**
     * Find an aggregate by its unique identifier.
     */
    Optional<T> findById(ID id);

    /**
     * Find all aggregates.
     */
    List<T> findAll();

    /**
     * Find aggregates by specification.
     */
    List<T> findBySpecification(Specification<T> spec);

    /**
     * Save an aggregate (insert or update).
     */
    T save(T aggregate);

    /**
     * Save all aggregates in batch.
     */
    List<T> saveAll(List<T> aggregates);

    /**
     * Delete an aggregate by its identifier.
     */
    void deleteById(ID id);

    /**
     * Delete an aggregate.
     */
    void delete(T aggregate);

    /**
     * Delete all aggregates matching the specification.
     */
    void deleteBySpecification(Specification<T> spec);

    /**
     * Check if an aggregate exists by its identifier.
     */
    boolean existsById(ID id);

    /**
     * Count all aggregates.
     */
    long count();

    /**
     * Count aggregates matching the specification.
     */
    long countBySpecification(Specification<T> spec);

    /**
     * Find aggregates with pagination.
     */
    PageResult<T> findPaged(PageRequest pageRequest);

    /**
     * Find aggregates with pagination by specification.
     */
    PageResult<T> findPagedBySpecification(Specification<T> spec, PageRequest pageRequest);

    /**
     * Page request record
     */
    record PageRequest(
        int pageNumber,
        int pageSize,
        String sortBy,
        SortDirection sortDirection
    ) {
        public enum SortDirection { ASC, DESC }

        public static PageRequest of(int pageNumber, int pageSize) {
            return new PageRequest(pageNumber, pageSize, null, null);
        }

        public static PageRequest of(int pageNumber, int pageSize, String sortBy, SortDirection direction) {
            return new PageRequest(pageNumber, pageSize, sortBy, direction);
        }

        public int getOffset() {
            return pageNumber * pageSize;
        }
    }

    /**
     * Page result record
     */
    record PageResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
    ) {
        public static <T> PageResult<T> of(List<T> content, int pageNumber, int pageSize, long total) {
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new PageResult<T>(
                content,
                pageNumber,
                pageSize,
                total,
                totalPages,
                pageNumber < totalPages - 1,
                pageNumber > 0
            );
        }
    }
}