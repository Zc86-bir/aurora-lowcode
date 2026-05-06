package com.aurora.core.infrastructure.webhook;

import com.aurora.core.architecture.DomainEvent;
import com.aurora.core.contract.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Webhook Configuration — registers the WebhookDispatcher as a DomainEvent subscriber.
 *
 * <p>On application startup, subscribes to all DomainEvent types via the
 * EventBus. When any domain event fires, the WebhookDispatcher checks for
 * matching webhook endpoints and delivers the event.
 */
@Component
public class WebhookConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebhookConfiguration.class);

    private static final Set<Class<? extends DomainEvent>> ALL_EVENT_TYPES = Set.of(
            DomainEvent.Created.class,
            DomainEvent.Updated.class,
            DomainEvent.Deleted.class,
            DomainEvent.StatusChanged.class,
            DomainEvent.Versioned.class,
            DomainEvent.ExecutionEvent.class
    );

    private final WebhookDispatcher dispatcher;
    private final EventBus eventBus;

    public WebhookConfiguration(WebhookDispatcher dispatcher, EventBus eventBus) {
        this.dispatcher = dispatcher;
        this.eventBus = eventBus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        eventBus.subscribeMulti(ALL_EVENT_TYPES, new EventBus.MultiEventHandler() {
            @Override
            public void handle(DomainEvent event) {
                dispatcher.onDomainEvent(event);
            }

            @Override
            public Set<Class<? extends DomainEvent>> getSupportedEventTypes() {
                return ALL_EVENT_TYPES;
            }
        });

        log.info("Webhook dispatcher registered for all DomainEvent types");
    }
}
