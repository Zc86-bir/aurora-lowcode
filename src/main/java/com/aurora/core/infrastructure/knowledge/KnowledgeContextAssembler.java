package com.aurora.core.infrastructure.knowledge;

import com.aurora.core.infrastructure.vector.TenantAwareVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Knowledge Context Assembler — resolves enterprise knowledge context
 * for RAG-augmented generation.
 *
 * <p>Calls {@link TenantAwareVectorStore} with mandatory scope and visibility
 * filters, then assembles retrieved content into a formatted context block.
 *
 * <p>Empty results return an empty string — callers must handle the fallback
 * gracefully without throwing.
 */
@Service
public class KnowledgeContextAssembler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeContextAssembler.class);

    private final TenantAwareVectorStore vectorStore;

    public KnowledgeContextAssembler(TenantAwareVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Assemble enterprise context from vector search results.
     *
     * @param embedding           query embedding vector
     * @param allowedScopes       permitted knowledge_scope values (TENANT, PROJECT, MODULE)
     * @param visibilityPolicies  permitted visibility_policy values
     * @return formatted context string, or empty string if no results
     */
    public String assemble(double[] embedding,
                           Set<String> allowedScopes,
                           Set<String> visibilityPolicies) {
        List<TenantAwareVectorStore.SearchResult> results =
                vectorStore.similaritySearch(embedding, 3, 0.75,
                        allowedScopes, visibilityPolicies);

        if (results.isEmpty()) {
            log.debug("No RAG context found for scopes={}", allowedScopes);
            return "";
        }

        return results.stream()
                .sorted(Comparator
                        .comparingInt((TenantAwareVectorStore.SearchResult r) -> scopeRank(r.knowledgeScope()))
                        .thenComparing(Comparator.comparingDouble(
                                TenantAwareVectorStore.SearchResult::similarity).reversed()))
                .limit(3)
                .map(r -> String.format("[%s/%s] %s",
                        r.knowledgeScope(),
                        r.moduleId() != null ? r.moduleId() : (r.projectId() != null ? r.projectId() : "-"),
                        r.content()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private static int scopeRank(String scope) {
        return switch (scope) {
            case "MODULE" -> 0;
            case "PROJECT" -> 1;
            default -> 2; // TENANT
        };
    }
}
