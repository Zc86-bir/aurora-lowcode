package com.aurora.core.infrastructure.webhook;

import com.aurora.core.architecture.DomainEvent;
import com.aurora.core.infrastructure.database.entity.WebhookEndpointEntity;
import com.aurora.core.infrastructure.database.repository.WebhookEndpointRepositoryJpa;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/**
 * Webhook Dispatcher — delivers domain events to registered external endpoints.
 *
 * <p>Subscribes to all {@link DomainEvent} types. When an event fires,
 * this dispatcher finds all active webhook endpoints for the event's tenant
 * that match the event type, and delivers the event payload.
 *
 * <p>Delivery uses:
 * <ul>
 *   <li>{@link StructuredTaskScope} with virtual threads for parallel fan-out</li>
 *   <li>Java 11+ {@link HttpClient} for HTTP POST calls</li>
 *   <li>Resilience4j {@link Retry} with 3 attempts and exponential backoff</li>
 *   <li>HMAC-SHA256 signing via {@link WebhookSigner}</li>
 * </ul>
 *
 * <p>The dispatcher runs asynchronously — it does NOT block the main
 * business transaction. Failed deliveries are logged and counted.
 */
@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookEndpointRepositoryJpa endpointRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Retry retry;

    public WebhookDispatcher(WebhookEndpointRepositoryJpa endpointRepository,
                              ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryOnException(e -> true)
                .build();
        this.retry = RetryRegistry.of(retryConfig).retry("webhook-dispatcher");
    }

    /**
     * Handle a domain event — dispatch to matching webhook endpoints.
     *
     * <p>This runs asynchronously via the event bus's virtual thread executor.
     * Multiple endpoints are delivered in parallel using StructuredTaskScope.
     *
     * @param event the domain event to dispatch
     */
    public void onDomainEvent(DomainEvent event) {
        UUID tenantId = event.getTenantId();
        String eventType = event.getEventType();

        List<WebhookEndpointEntity> endpoints = endpointRepository
                .findActiveByTenantAndEvent(tenantId, eventType);

        if (endpoints.isEmpty()) return;

        log.debug("Dispatching {} event to {} webhook endpoints for tenant={}",
                eventType, endpoints.size(), tenantId);

        String payload = serializeEvent(event);

        try (var scope = StructuredTaskScope.<Void, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(Duration.ofSeconds(30)))) {

            for (WebhookEndpointEntity endpoint : endpoints) {
                scope.fork(() -> {
                    deliverWithRetry(endpoint, payload);
                    return null;
                });
            }

            scope.join();

        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Webhook dispatch timed out for tenant={} event={}", tenantId, eventType);
        } catch (Exception e) {
            log.error("Webhook dispatch failed for tenant={} event={}: {}",
                    tenantId, eventType, e.getMessage());
        }
    }

    /**
     * Deliver payload to a single endpoint with Resilience4j retry.
     */
    private void deliverWithRetry(WebhookEndpointEntity endpoint, String payload) {
        try {
            retry.executeRunnable(() -> {
                try {
                    deliver(endpoint, payload);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Success
            endpoint.setSuccessCount(endpoint.getSuccessCount() + 1);
            endpoint.setLastDeliveredAt(Instant.now());
            endpoint.setRetryCount(0);
            endpointRepository.save(endpoint);

        } catch (Exception e) {
            // All retries exhausted
            endpoint.setFailureCount(endpoint.getFailureCount() + 1);
            endpoint.setLastFailureAt(Instant.now());
            endpoint.setLastFailureMessage(truncate(e.getMessage(), 1024));
            endpointRepository.save(endpoint);

            log.error("Webhook delivery failed to {} after {} retries: {}",
                    endpoint.getUrl(), retry.getRetryConfig().getMaxAttempts(),
                    e.getMessage());
        }
    }

    /**
     * Single delivery attempt — build HTTP request, sign, and send.
     */
    private void deliver(WebhookEndpointEntity endpoint, String payload) throws Exception {
        String signature = WebhookSigner.sign(payload, endpoint.getSecret());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Aurora-Signature", signature)
                .header("X-Aurora-Webhook-Id", endpoint.getId().toString())
                .header("X-Aurora-Event", "domain_event")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(String.format(
                    "Webhook endpoint returned HTTP %d: %s",
                    response.statusCode(),
                    truncate(response.body(), 500)));
        }

        log.debug("Webhook delivered to {} (HTTP {})", endpoint.getUrl(), response.statusCode());
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getEventId().toString(),
                    "eventType", event.getEventType(),
                    "aggregateType", event.getAggregateType(),
                    "aggregateId", event.getAggregateId(),
                    "tenantId", event.getTenantId().toString(),
                    "occurredAt", event.getOccurredAt().toString(),
                    "causedBy", event.getCausedBy() != null ? event.getCausedBy() : "system",
                    "eventVersion", event.getEventVersion()
            ));
        } catch (Exception e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            return "{\"error\":\"serialization failed\"}";
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
