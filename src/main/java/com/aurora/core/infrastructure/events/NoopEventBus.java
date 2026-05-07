package com.aurora.core.infrastructure.events;

import com.aurora.core.architecture.DomainEvent;
import com.aurora.core.contract.EventBus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
@Profile("dev")
public class NoopEventBus implements EventBus {

    @Override
    public void publish(DomainEvent event) {}

    @Override
    public void publish(UUID tenantId, DomainEvent event) {}

    @Override
    public CompletableFuture<Void> publishAsync(UUID tenantId, DomainEvent event) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void publishBatch(UUID tenantId, List<DomainEvent> events) {}

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {}

    @Override
    public <T extends DomainEvent> void subscribe(UUID tenantId, Class<T> eventType, EventHandler<T> handler) {}

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler, Predicate<T> filter) {}

    @Override
    public <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {}

    @Override
    public void subscribeMulti(Set<Class<? extends DomainEvent>> eventTypes, MultiEventHandler handler) {}

    @Override
    public <T extends DomainEvent, R extends DomainEvent> CompletableFuture<R> request(UUID tenantId, T requestEvent, Class<R> responseType, Duration timeout) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("NoopEventBus does not support request-response"));
    }

    @Override
    public List<DomainEvent> getEventHistory(UUID tenantId, String aggregateId, int limit) {
        return List.of();
    }

    @Override
    public void replayEvents(UUID tenantId, String aggregateId, List<DomainEvent> events, Consumer<DomainEvent> handler) {}

    @Override
    public int getSubscriberCount(Class<? extends DomainEvent> eventType) {
        return 0;
    }

    @Override
    public EventBusStatistics getStatistics(UUID tenantId) {
        return new EventBusStatistics(
                tenantId,
                0,
                0,
                0,
                0,
                0.0,
                0,
                0,
                Map.of(),
                Map.of(),
                Instant.now()
        );
    }

    @Override
    public void pauseProcessing(UUID tenantId) {}

    @Override
    public void resumeProcessing(UUID tenantId) {}

    @Override
    public List<DeadLetterEntry> getDeadLetters(UUID tenantId, int limit) {
        return List.of();
    }

    @Override
    public void retryDeadLetter(UUID tenantId, UUID deadLetterId) {}
}
