package com.aurora.core.contract;

import com.aurora.core.architecture.DomainEvent;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.time.Duration;

/**
 * Event Bus Interface
 *
 * Provides event-driven communication with virtual thread support.
 * Supports synchronous and asynchronous event processing with
 * structured concurrency for parallel handler execution.
 */
public interface EventBus {

    /**
     * Publish an event.
     */
    void publish(DomainEvent event);

    /**
     * Publish an event with tenant context.
     */
    void publish(UUID tenantId, DomainEvent event);

    /**
     * Publish event asynchronously.
     */
    CompletableFuture<Void> publishAsync(UUID tenantId, DomainEvent event);

    /**
     * Publish multiple events in batch.
     */
    void publishBatch(UUID tenantId, List<DomainEvent> events);

    /**
     * Subscribe to events of a specific type.
     */
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);

    /**
     * Subscribe with tenant filter.
     */
    <T extends DomainEvent> void subscribe(UUID tenantId, Class<T> eventType, EventHandler<T> handler);

    /**
     * Subscribe with filter predicate.
     */
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler,
                                            Predicate<T> filter);

    /**
     * Unsubscribe a handler.
     */
    <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler);

    /**
     * Subscribe to multiple event types.
     */
    void subscribeMulti(Set<Class<? extends DomainEvent>> eventTypes, MultiEventHandler handler);

    /**
     * Request-response pattern (wait for response event).
     */
    <T extends DomainEvent, R extends DomainEvent> CompletableFuture<R> request(
        UUID tenantId,
        T requestEvent,
        Class<R> responseType,
        Duration timeout
    );

    /**
     * Get event history for replay.
     */
    List<DomainEvent> getEventHistory(UUID tenantId, String aggregateId, int limit);

    /**
     * Replay events from history.
     */
    void replayEvents(UUID tenantId, String aggregateId,
                      List<DomainEvent> events,
                      Consumer<DomainEvent> handler);

    /**
     * Get subscriber count for an event type.
     */
    int getSubscriberCount(Class<? extends DomainEvent> eventType);

    /**
     * Get event bus statistics.
     */
    EventBusStatistics getStatistics(UUID tenantId);

    /**
     * Pause event processing for a tenant.
     */
    void pauseProcessing(UUID tenantId);

    /**
     * Resume event processing for a tenant.
     */
    void resumeProcessing(UUID tenantId);

    /**
     * Get dead letter queue events.
     */
    List<DeadLetterEntry> getDeadLetters(UUID tenantId, int limit);

    /**
     * Retry a dead letter event.
     */
    void retryDeadLetter(UUID tenantId, UUID deadLetterId);

    // Value types

    /**
     * Event handler interface
     */
    interface EventHandler<T extends DomainEvent> {
        void handle(T event);
        void onError(T event, Exception error);
        int getOrder();
        boolean isEnabled();
    }

    /**
     * Multi-event handler interface
     */
    interface MultiEventHandler {
        void handle(DomainEvent event);
        Set<Class<? extends DomainEvent>> getSupportedEventTypes();
    }

    /**
     * Event bus statistics
     */
    record EventBusStatistics(
        UUID tenantId,
        long totalPublished,
        long totalProcessed,
        long totalFailed,
        long deadLetterCount,
        double averageProcessingTimeMs,
        long currentQueueSize,
        int activeSubscribers,
        Map<String, Long> countByEventType,
        Map<String, Double> avgTimeByEventType,
        java.time.Instant lastEventAt
    ) {}

    /**
     * Dead letter entry
     */
    record DeadLetterEntry(
        UUID deadLetterId,
        UUID tenantId,
        DomainEvent event,
        String errorMessage,
        List<String> stackTrace,
        int retryCount,
        java.time.Instant firstFailedAt,
        java.time.Instant lastFailedAt,
        java.time.Instant nextRetryAt,
        DeadLetterStatus status
    ) {
        public enum DeadLetterStatus {
            PENDING, RETRYING, EXHAUSTED, RESOLVED, IGNORED
        }
    }

    /**
     * Event subscription metadata
     */
    record EventSubscription(
        UUID subscriptionId,
        UUID tenantId,
        Class<? extends DomainEvent> eventType,
        String handlerName,
        int order,
        boolean enabled,
        boolean async,
        int maxRetries,
        Duration retryDelay,
        java.time.Instant createdAt
    ) {}

    /**
     * Event envelope for transport
     */
    record EventEnvelope(
        UUID eventId,
        UUID tenantId,
        String aggregateId,
        String aggregateType,
        DomainEvent event,
        java.time.Instant timestamp,
        String correlationId,
        String causationId,
        Map<String, String> headers,
        int version
    ) {}
}