package com.aurora.core.infrastructure.vector;

import com.aurora.core.contract.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class TenantAwareVectorStore {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareVectorStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final TenantContext tenantContext;
    private volatile Boolean vectorEnabled;

    public TenantAwareVectorStore(JdbcTemplate jdbcTemplate, TenantContext tenantContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantContext = tenantContext;
    }

    /**
     * Execute a tenant-scoped similarity search.
     *
     * Uses PostgreSQL array binding (ANY(?)) to prevent SQL injection
     * through scope and policy sets.
     */
    public List<SearchResult> similaritySearch(double[] embedding, int topK,
                                                double similarityThreshold,
                                                Set<String> allowedScopes,
                                                Set<String> visibilityPolicies) {
        UUID tenantId = tenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context required for vector search");
        }

        if (!isVectorEnabled()) {
            log.debug("Skipping vector search because pgvector is unavailable");
            return List.of();
        }

        String embeddingStr = "[" + doubleArrayToString(embedding) + "]";

        String sql = """
            SELECT content, knowledge_scope, project_id, module_id,
                   1 - (embedding <=> ?::vector) AS similarity
            FROM vector_store
            WHERE tenant_id = ?
              AND knowledge_scope = ANY(?)
              AND visibility_policy = ANY(?)
              AND 1 - (embedding <=> ?::vector) >= ?
            ORDER BY CASE knowledge_scope
                     WHEN 'MODULE' THEN 0
                     WHEN 'PROJECT' THEN 1
                     ELSE 2
                     END,
                     embedding <=> ?::vector ASC
            LIMIT ?
            """;

        log.debug("Vector search: tenant={} topK={} threshold={}", tenantId, topK, similarityThreshold);

        return jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setString(1, embeddingStr);
                    ps.setObject(2, tenantId);
                    ps.setArray(3, ps.getConnection().createArrayOf("text", allowedScopes.toArray(new String[0])));
                    ps.setArray(4, ps.getConnection().createArrayOf("text", visibilityPolicies.toArray(new String[0])));
                    ps.setString(5, embeddingStr);
                    ps.setDouble(6, similarityThreshold);
                    ps.setString(7, embeddingStr);
                    ps.setInt(8, topK);
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

    private boolean isVectorEnabled() {
        Boolean cached = vectorEnabled;
        if (cached != null) {
            return cached;
        }

        boolean enabled = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')", Boolean.class));
        vectorEnabled = enabled;
        return enabled;
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
