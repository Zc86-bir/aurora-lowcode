package com.aurora.core.infrastructure.vector;

import com.aurora.core.contract.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tenant-Aware Vector Store — the ONLY allowed vector retrieval boundary.
 *
 * <p>Forces tenant_id, knowledge_scope, and visibility_policy filters on every
 * similarity search. No caller may execute raw vector queries outside this class.
 *
 * <p>Uses direct JDBC because Spring AI's PgVectorStore metadata-filter API
 * does not provide strong enough guarantees for mandatory tenant injection.
 */
@Component
public class TenantAwareVectorStore {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareVectorStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final TenantContext tenantContext;

    public TenantAwareVectorStore(JdbcTemplate jdbcTemplate, TenantContext tenantContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantContext = tenantContext;
    }

    /**
     * Execute a tenant-scoped similarity search.
     *
     * @param embedding            embedding vector as float array
     * @param topK                 max results
     * @param similarityThreshold  minimum cosine distance converted to similarity (0..1)
     * @param allowedScopes        permitted knowledge_scope values (TENANT, PROJECT, MODULE)
     * @param visibilityPolicies   permitted visibility_policy values
     * @return ordered search results (MODULE > PROJECT > TENANT, then by similarity DESC)
     */
    public List<SearchResult> similaritySearch(double[] embedding, int topK,
                                                double similarityThreshold,
                                                Set<String> allowedScopes,
                                                Set<String> visibilityPolicies) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context required for vector search");
        }

        String scopeList = allowedScopes.stream()
                .map(s -> "'" + s.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
        String policyList = visibilityPolicies.stream()
                .map(p -> "'" + p.replace("'", "''") + "'")
                .collect(Collectors.joining(","));

        String embeddingStr = "[" + doubleArrayToString(embedding) + "]";

        String sql = """
            SELECT content, knowledge_scope, project_id, module_id,
                   1 - (embedding <=> ?::vector) AS similarity
            FROM vector_store
            WHERE tenant_id = ?
              AND knowledge_scope IN (%s)
              AND visibility_policy IN (%s)
              AND 1 - (embedding <=> ?::vector) >= ?
            ORDER BY CASE knowledge_scope
                     WHEN 'MODULE' THEN 0
                     WHEN 'PROJECT' THEN 1
                     ELSE 2
                     END,
                     embedding <=> ?::vector ASC
            LIMIT ?
            """.formatted(scopeList, policyList);

        log.debug("Vector search: tenant={} topK={} threshold={}", tenantId, topK, similarityThreshold);

        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, embeddingStr);
                    ps.setObject(2, tenantId);
                    ps.setString(3, embeddingStr);
                    ps.setDouble(4, similarityThreshold);
                    ps.setString(5, embeddingStr);
                    ps.setInt(6, topK);
                },
                (rs, rowNum) -> new SearchResult(
                        rs.getString("content"),
                        rs.getString("knowledge_scope"),
                        rs.getString("project_id"),
                        rs.getString("module_id"),
                        rs.getDouble("similarity")
                )
        );
    }

    private static String doubleArrayToString(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    public record SearchResult(
            String content,
            String knowledgeScope,
            String projectId,
            String moduleId,
            double similarity
    ) {}
}
