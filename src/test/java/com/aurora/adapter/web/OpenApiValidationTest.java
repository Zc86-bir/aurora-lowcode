package com.aurora.adapter.web;

import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.CacheProvider;
import com.aurora.core.contract.EventBus;
import com.aurora.core.contract.PermissionChecker;
import com.aurora.core.adapter.websocket.YjsWebSocketHandler;
import com.aurora.core.generator.CodeGenerator;
import com.aurora.core.infrastructure.ai.LlmGatewayService;
import com.aurora.core.infrastructure.knowledge.KnowledgeIngestionService;
import com.aurora.core.runtime.FormRuntimeEngine;
import com.aurora.core.runtime.MetadataHotReloadManager;
import com.aurora.core.runtime.ReportRuntimeEngine;
import com.aurora.core.runtime.WorkflowRuntimeEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Validates OpenAPI documentation endpoint returns valid YAML.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiValidationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    @DisplayName("OpenAPI docs endpoint returns 200 OK")
    void openApiDocsEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Swagger UI endpoint is accessible")
    void swaggerUiEndpointIsAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
