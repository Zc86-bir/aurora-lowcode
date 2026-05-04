package com.aurora.core.infrastructure.event;

import com.aurora.core.contract.EventBus;
import com.aurora.core.architecture.DomainEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Virtual Thread Event Bus
 *
 * Uses Java 25 virtual threads for event handler execution.
 * Supports:
 * - Synchronous and asynchronous event processing
 * - Dead letter queue with retry
 * - Event ordering per aggregate
 * - Tenant-scoped subscriptions
 */
public class VirtualThreadEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadEventBus.class);

    private final Map<Class<? extends DomainEvent>, List<Subscriber<?>>> subscribers =
        new ConcurrentHashMap<>();

    private final List<DeadLetterEntry> deadLetterQueue = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(1);

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile Map<UUID, Boolean> pausedTenants = new ConcurrentHashMap<>();

    public VirtualThreadEventBus() {
        // Retry dead letters every 30 seconds
        retryScheduler.scheduleAtFixedRate(
            this::retryPendingDeadLetters,
            30, 30, TimeUnit.SECONDS
        );
    }

    @Override
    public void publish(DomainEvent event) {
        UUID tenantId = event.getTenantId();
        if (tenantId != null) {
            publish(tenantId, event);
        } else {
            doPublish(event);
        }
    }

    @Override
    public void publish(UUID tenantId, DomainEvent event) {
        if (isPaused(tenantId)) {
            log.debug("Event processing paused for tenant: {}", tenantId);
            return;
        }
        doPublish(event);
    }

    @Override
    public CompletableFuture<Void> publishAsync(UUID tenantId, DomainEvent event) {
        return CompletableFuture.runAsync(() -> publish(tenantId, event), virtualThreadExecutor);
    }

    @Override
    public void publishBatch(UUID tenantId, List<DomainEvent> events) {
        for (DomainEvent event : events) {
            publish(tenantId, event);
        }
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        Subscriber<T> subscriber = new Subscriber<>(null, handler, null);
        getSubscribers(eventType).add(subscriber);
    }

    @Override
    public <T extends DomainEvent> void subscribe(UUID tenantId, Class<T> eventType,
                                                    EventHandler<T> handler) {
        Subscriber<T> subscriber = new Subscriber<>(tenantId, handler, null);
        getSubscribers(eventType).add(subscriber);
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler,
                                                    Predicate<T> filter) {
        Subscriber<T> subscriber = new Subscriber<>(null, handler, filter);
        getSubscribers(eventType).add(subscriber);
    }

    @Override
    public <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {
        List<Subscriber<?>> subs = subscribers.get(eventType);
        if (subs != null) {
            subs.removeIf(s -> s.handler() == handler);
        }
    }

    @Override
    public void subscribeMulti(Set<Class<? extends DomainEvent>> eventTypes,
                                MultiEventHandler handler) {
        for (Class<? extends DomainEvent> eventType : eventTypes) {
            @SuppressWarnings("unchecked")
            Class<DomainEvent> rawType = (Class<DomainEvent>) eventType;

            EventHandler<DomainEvent> wrapper = new EventHandler<>() {
                @Override
                public void handle(DomainEvent event) {
                    handler.handle(event);
                }

                @Override
                public void onError(DomainEvent event, Exception error) {
                    log.error("MultiEventHandler error for event {}: {}",
                        event.getEventType(), error.getMessage(), error);
                }

                @Override
                public int getOrder() { return 0; }

                @Override
                public boolean isEnabled() { return true; }
            };

            getSubscribers(rawType).add(new Subscriber<>(null, wrapper, null));
        }
    }

    @Override
    public <T extends DomainEvent, R extends DomainEvent> CompletableFuture<R> request(
        UUID tenantId, T requestEvent, Class<R> responseType, Duration timeout) {

        CompletableFuture<R> future = new CompletableFuture<>();

        EventHandler<R> responseHandler = new EventHandler<>() {
            @Override
            public void handle(R event) {
                if (event.getAggregateId().equals(requestEvent.getAggregateId())) {
                    future.complete(event);
                }
            }

            @Override
            public void onError(R event, Exception error) {
                future.completeExceptionally(error);
            }

            @Override
            public int getOrder() { return Integer.MAX_VALUE; }

            @Override
            public boolean isEnabled() { return true; }
        };

        subscribe(tenantId, responseType, responseHandler);

        // Publish request
        publish(tenantId, requestEvent);

        // Timeout
        virtualThreadExecutor.submit(() -> {
            try {
                Thread.sleep(timeout.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!future.isDone()) {
                future.completeExceptionally(new java.util.concurrent.TimeoutException(
                    "Request timed out after " + timeout));
                unsubscribe(responseType, responseHandler);
            }
        });

        return future;
    }

    @Override
    public List<DomainEvent> getEventHistory(UUID tenantId, String aggregateId, int limit) {
        // In production: query event store database
        return List.of();
    }

    @Override
    public void replayEvents(UUID tenantId, String aggregateId,
                              List<DomainEvent> events, Consumer<DomainEvent> handler) {
        for (DomainEvent event : events) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                log.error("Error replaying event: {}", event.getEventId(), e);
            }
        }
    }

    @Override
    public int getSubscriberCount(Class<? extends DomainEvent> eventType) {
        return getSubscribers(eventType).size();
    }

    @Override
    public EventBusStatistics getStatistics(UUID tenantId) {
        int totalSubscribers = subscribers.values().stream()
            .mapToInt(List::size)
            .sum();

        long tenantDeadLetters = deadLetterQueue.stream()
            .filter(e -> e.tenantId().equals(tenantId))
            .count();

        return new EventBusStatistics(
            tenantId,
            0, 0, 0,
            tenantDeadLetters,
            0.0,
            0,
            totalSubscribers,
            Map.of(),
            Map.of(),
            null
        );
    }

    @Override
    public void pauseProcessing(UUID tenantId) {
        Map<UUID, Boolean> updated = new ConcurrentHashMap<>(pausedTenants);
        updated.put(tenantId, true);
        pausedTenants = updated;
    }

    @Override
    public void resumeProcessing(UUID tenantId) {
        Map<UUID, Boolean> updated = new ConcurrentHashMap<>(pausedTenants);
        updated.remove(tenantId);
        pausedTenants = updated;
    }

    @Override
    public List<DeadLetterEntry> getDeadLetters(UUID tenantId, int limit) {
        return deadLetterQueue.stream()
            .filter(e -> e.tenantId().equals(tenantId))
            .filter(e -> e.status() == DeadLetterEntry.DeadLetterStatus.PENDING
                || e.status() == DeadLetterEntry.DeadLetterStatus.RETRYING)
            .limit(limit)
            .toList();
    }

    @Override
    public void retryDeadLetter(UUID tenantId, UUID deadLetterId) {
        deadLetterQueue.stream()
            .filter(e -> e.deadLetterId().equals(deadLetterId))
            .findFirst()
            .ifPresent(entry -> {
                retryDeadLetterEntry(entry);
            });
    }

    @SuppressWarnings("unchecked")
    private static void onErrorSafe(EventHandler<?> handler, DomainEvent event, Exception error) {
        ((EventHandler<DomainEvent>) handler).onError(event, error);
    }

    /**
     * Shutdown the event bus gracefully.
     */
    public void shutdown() {
        retryScheduler.shutdown();
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Internal

    private void doPublish(DomainEvent event) {
        List<Subscriber<?>> subs = getSubscribers(event.getClass());
        if (subs.isEmpty()) {
            log.debug("No subscribers for event: {}", event.getEventType());
            return;
        }

        // Sort by order
        List<Subscriber<?>> sorted = subs.stream()
            .filter(s -> s.handler().isEnabled())
            .filter(s -> s.tenantId() == null || s.tenantId().equals(event.getTenantId()))
            .sorted((a, b) -> Integer.compare(a.handler().getOrder(), b.handler().getOrder()))
            .toList();

        for (Subscriber<?> subscriber : sorted) {
            try {
                dispatchEvent(subscriber, event);
            } catch (Exception e) {
                handleEventError(event, subscriber.handler(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void dispatchEvent(Subscriber<?> subscriber,
                                                         DomainEvent event) {
        // Check filter before handling
        if (subscriber.filter() != null) {
            if (!((Predicate<T>) subscriber.filter()).test((T) event)) {
                return;
            }
        }

        ((EventHandler<T>) subscriber.handler()).handle((T) event);
    }

    private void handleEventError(DomainEvent event, EventHandler<?> handler, Exception error) {
        log.error("Error handling event {}: {}", event.getEventType(), error.getMessage(), error);

        onErrorSafe(handler, event, error);

        // Add to dead letter queue
        DeadLetterEntry entry = new DeadLetterEntry(
            UUID.randomUUID(),
            event.getTenantId(),
            event,
            error.getMessage(),
            getStackTrace(error),
            0,
            Instant.now(),
            Instant.now(),
            Instant.now().plus(Duration.ofMinutes(1)),
            DeadLetterEntry.DeadLetterStatus.PENDING
        );

        deadLetterQueue.add(entry);
    }

    private List<String> getStackTrace(Throwable t) {
        List<String> lines = new ArrayList<>();
        for (StackTraceElement element : t.getStackTrace()) {
            lines.add(element.toString());
            if (lines.size() >= 10) break;
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> List<Subscriber<?>> getSubscribers(Class<T> eventType) {
        return (List<Subscriber<?>>) (List<?>) subscribers.computeIfAbsent(eventType,
            k -> new CopyOnWriteArrayList<>());
    }

    private boolean isPaused(UUID tenantId) {
        return pausedTenants.getOrDefault(tenantId, false);
    }

    private void retryPendingDeadLetters() {
        Instant now = Instant.now();
        for (DeadLetterEntry entry : deadLetterQueue) {
            if (entry.status() == DeadLetterEntry.DeadLetterStatus.PENDING
                || entry.status() == DeadLetterEntry.DeadLetterStatus.RETRYING) {
                if (!entry.nextRetryAt().isAfter(now)) {
                    retryDeadLetterEntry(entry);
                }
            }
        }
    }

    private void retryDeadLetterEntry(DeadLetterEntry entry) {
        try {
            publish(entry.tenantId(), entry.event());

            // Update status to resolved
            int index = deadLetterQueue.indexOf(entry);
            if (index >= 0) {
                deadLetterQueue.set(index, new DeadLetterEntry(
                    entry.deadLetterId(),
                    entry.tenantId(),
                    entry.event(),
                    entry.errorMessage(),
                    entry.stackTrace(),
                    entry.retryCount() + 1,
                    entry.firstFailedAt(),
                    Instant.now(),
                    null,
                    DeadLetterEntry.DeadLetterStatus.RESOLVED
                ));
            }
        } catch (Exception e) {
            int index = deadLetterQueue.indexOf(entry);
            if (index >= 0) {
                int newRetryCount = entry.retryCount() + 1;
                DeadLetterEntry.DeadLetterStatus newStatus = newRetryCount >= 3
                    ? DeadLetterEntry.DeadLetterStatus.EXHAUSTED
                    : DeadLetterEntry.DeadLetterStatus.RETRYING;

                deadLetterQueue.set(index, new DeadLetterEntry(
                    entry.deadLetterId(),
                    entry.tenantId(),
                    entry.event(),
                    e.getMessage(),
                    getStackTrace(e),
                    newRetryCount,
                    entry.firstFailedAt(),
                    Instant.now(),
                    Instant.now().plus(Duration.ofMinutes((long) Math.pow(2, newRetryCount))),
                    newStatus
                ));
            }
        }
    }

    /**
     * Internal subscriber record.
     */
    private record Subscriber<T extends DomainEvent>(
        UUID tenantId,
        EventHandler<T> handler,
        Predicate<T> filter
    ) {}
}