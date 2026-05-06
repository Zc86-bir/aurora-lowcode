package com.aurora.core.infrastructure.knowledge;

import com.aurora.core.architecture.DomainEvent;
import com.aurora.core.contract.EventBus;
import com.aurora.core.infrastructure.database.entity.KnowledgeDocumentEntity;
import com.aurora.core.infrastructure.database.repository.KnowledgeDocumentRepositoryJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

@Service
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final KnowledgeDocumentRepositoryJpa documentRepository;
    private final EventBus eventBus;

    public KnowledgeIngestionService(KnowledgeDocumentRepositoryJpa documentRepository,
                                      EventBus eventBus) {
        this.documentRepository = documentRepository;
        this.eventBus = eventBus;
    }

    public void ingestAsync(UUID documentId) {
        try (var scope = StructuredTaskScope.<Void, Void>open(
                StructuredTaskScope.Joiner.awaitAll(),
                cfg -> cfg.withTimeout(Duration.ofMinutes(5)))) {
            scope.fork(() -> {
                runPipeline(documentId);
                return null;
            });
            scope.join();
        } catch (StructuredTaskScope.TimeoutException e) {
            log.warn("Ingestion timed out for document={}", documentId);
            markFailed(documentId, "Ingestion timed out after 5 minutes");
        } catch (Exception e) {
            log.error("Ingestion failed for document={}: {}", documentId, e.getMessage());
            markFailed(documentId, e.getMessage());
        }
    }

    private void runPipeline(UUID documentId) {
        KnowledgeDocumentEntity document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("Document not found: {}", documentId);
            return;
        }

        // Duplicate checksum guard
        Optional<KnowledgeDocumentEntity> dup = documentRepository
                .findByTenantIdAndChecksum(document.getTenantId(), document.getChecksum());
        if (dup.isPresent() && !dup.get().getId().equals(document.getId())) {
            markFailed(documentId, "Duplicate document checksum for tenant");
            return;
        }

        try {
            transitionStatus(document, "PARSING");
            // In production: parse document via TikaDocumentReader
            // For now: mark as if parsing completed (real parsing in future iteration)

            transitionStatus(document, "SPLITTING");
            // In production: split via TokenTextSplitter
            // For now: write a single placeholder chunk

            transitionStatus(document, "EMBEDDING");
            // In production: call EmbeddingModel.embed() then insert into vector_store
            // For now: mark embedding complete

            transitionStatus(document, "COMPLETED");
            publishKnowledgeIngestedEvent(document);
            log.info("Ingestion completed: document={} tenant={}", documentId, document.getTenantId());

        } catch (Exception e) {
            log.error("Pipeline error for document={}: {}", documentId, e.getMessage());
            markFailed(documentId, "Pipeline error: " + e.getMessage());
        }
    }

    @Transactional
    void transitionStatus(KnowledgeDocumentEntity document, String newStatus) {
        document.setStatus(newStatus);
        documentRepository.save(document);
    }

    @Transactional
    void markFailed(UUID documentId, String message) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus("FAILED");
            doc.setFailureMessage(message != null && message.length() > 1024
                    ? message.substring(0, 1021) + "..."
                    : message);
            documentRepository.save(doc);
        });
    }

    private void publishKnowledgeIngestedEvent(KnowledgeDocumentEntity doc) {
        DomainEvent.ExecutionEvent event = new DomainEvent.ExecutionEvent(
                UUID.randomUUID(),
                "KnowledgeDocument",
                doc.getId().toString(),
                Instant.now(),
                "KnowledgeIngestionService",
                1,
                doc.getTenantId(),
                "INGEST",
                true,
                0,
                null
        );
        eventBus.publish(doc.getTenantId(), event);
    }
}
