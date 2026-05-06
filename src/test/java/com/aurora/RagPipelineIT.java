package com.aurora;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.CacheProvider;
import com.aurora.core.contract.EventBus;
import com.aurora.core.contract.PermissionChecker;
import com.aurora.core.adapter.websocket.YjsWebSocketHandler;
import com.aurora.core.generator.CodeGenerator;
import com.aurora.core.infrastructure.ai.LlmGatewayService;
import com.aurora.core.infrastructure.knowledge.KnowledgeContextAssembler;
import com.aurora.core.infrastructure.knowledge.KnowledgeIngestionService;
import com.aurora.core.infrastructure.vector.TenantAwareVectorStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.aurora.core.runtime.FormRuntimeEngine;
import com.aurora.core.runtime.MetadataHotReloadManager;
import com.aurora.core.runtime.ReportRuntimeEngine;
import com.aurora.core.runtime.WorkflowRuntimeEngine;

import java.util.Set;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AuroraApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Tag("rag")
class RagPipelineIT {

    @Autowired
    private KnowledgeContextAssembler assembler;

    @Autowired
    private TenantContext tenantContext;

    @MockBean
    private FormRuntimeEngine formRuntimeEngine;

    @MockBean
    private ReportRuntimeEngine reportRuntimeEngine;

    @MockBean
    private WorkflowRuntimeEngine workflowRuntimeEngine;

    @MockBean
    private MetadataHotReloadManager metadataHotReloadManager;

    @MockBean
    private CodeGenerator codeGenerator;

    @MockBean
    private PermissionChecker permissionChecker;

    @MockBean
    private CacheProvider cacheProvider;

    @MockBean
    private AuditLogger auditLogger;

    @MockBean
    private EventBus eventBus;

    @MockBean
    private KnowledgeIngestionService knowledgeIngestionService;

    @MockBean
    private LlmGatewayService llmGatewayService;

    @MockBean
    private YjsWebSocketHandler yjsWebSocketHandler;

    @MockBean
    private TenantAwareVectorStore tenantAwareVectorStore;

    @Test
    @DisplayName("Returns empty context when no vector matches exist")
    void returnsEmptyContextWhenNoMatchesExist() {
        when(tenantAwareVectorStore.similaritySearch(any(double[].class), anyInt(), anyDouble(), anySet(), anySet()))
                .thenReturn(List.of());

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

        when(tenantAwareVectorStore.similaritySearch(any(double[].class), anyInt(), anyDouble(), anySet(), anySet()))
                .thenThrow(new IllegalStateException("Tenant context missing"));

        double[] query = new double[1536];
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> assembler.assemble(query,
                        Set.of("TENANT"),
                        Set.of("authenticated")));
        assertThat(e.getMessage()).contains("Tenant context");
    }

    @Test
    @DisplayName("KnowledgeContextAssembler bean is available")
    void assemblerBeanIsAvailable() {
        assertThat(assembler).isNotNull();
    }
}
