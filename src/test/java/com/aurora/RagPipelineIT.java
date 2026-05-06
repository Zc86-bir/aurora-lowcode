package com.aurora;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.knowledge.KnowledgeContextAssembler;
import com.aurora.core.infrastructure.vector.TenantAwareVectorStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("rag")
class RagPipelineIT {

    @Autowired
    private KnowledgeContextAssembler assembler;

    @Autowired
    private TenantAwareVectorStore vectorStore;

    @Autowired
    private TenantContext tenantContext;

    @Test
    @DisplayName("Returns empty context when no vector matches exist")
    void returnsEmptyContextWhenNoMatchesExist() {
        tenantContext.setContext(UUID.randomUUID(), UUID.randomUUID());

        double[] query = new double[1536];
        String result = assembler.assemble(query,
                Set.of("TENANT", "PROJECT", "MODULE"),
                Set.of("authenticated"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Denies search when tenant context is missing")
    void deniesSearchWhenTenantContextIsMissing() {
        tenantContext.clearContext();

        double[] query = new double[1536];
        try {
            assembler.assemble(query,
                    Set.of("TENANT"),
                    Set.of("authenticated"));
            Assertions.fail("Expected IllegalStateException for missing tenant context");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Tenant context");
        }
    }

    @Test
    @DisplayName("KnowledgeContextAssembler bean is available")
    void assemblerBeanIsAvailable() {
        assertThat(assembler).isNotNull();
    }
}
