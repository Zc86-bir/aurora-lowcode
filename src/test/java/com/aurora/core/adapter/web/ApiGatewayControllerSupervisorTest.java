package com.aurora.core.adapter.web;

import com.aurora.core.application.supervisor.SupervisorOrchestrator;
import com.aurora.core.application.supervisor.SupervisorRequest;
import com.aurora.core.application.supervisor.SupervisorResult;
import com.aurora.core.contract.AuditLogger;
import com.aurora.core.contract.CacheProvider;
import com.aurora.core.contract.PermissionChecker;
import com.aurora.core.contract.TenantContext;
import com.aurora.core.generator.CodeGenerator;
import com.aurora.core.runtime.FormRuntimeEngine;
import com.aurora.core.runtime.MetadataHotReloadManager;
import com.aurora.core.runtime.ReportRuntimeEngine;
import com.aurora.core.runtime.WorkflowRuntimeEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiGatewayControllerSupervisorTest {

    @Test
    void shouldDelegateCompositeAppGenerationToSupervisor() {
        SupervisorOrchestrator orchestrator = mock(SupervisorOrchestrator.class);
        when(orchestrator.execute(any(SupervisorRequest.class)))
                .thenReturn(new SupervisorResult(true, "req-1", "CRM", Instant.now(), Map.of(), List.of(), List.of(), null));

        ApiGatewayController controller = createController(orchestrator);

        var response = controller.generateCompositeApp(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of("prompt", "Build a CRM system")
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(orchestrator).execute(any(SupervisorRequest.class));
    }

    @Test
    void shouldRejectEmptyPrompt() {
        SupervisorOrchestrator orchestrator = mock(SupervisorOrchestrator.class);
        ApiGatewayController controller = createController(orchestrator);

        var response = controller.generateCompositeApp(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of("prompt", "")
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verifyNoInteractions(orchestrator);
    }

    @Test
    void shouldRejectMissingPrompt() {
        SupervisorOrchestrator orchestrator = mock(SupervisorOrchestrator.class);
        ApiGatewayController controller = createController(orchestrator);

        var response = controller.generateCompositeApp(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Map.of()
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verifyNoInteractions(orchestrator);
    }

    private static ApiGatewayController createController(SupervisorOrchestrator orchestrator) {
        return new ApiGatewayController(
                mock(FormRuntimeEngine.class),
                mock(ReportRuntimeEngine.class),
                mock(WorkflowRuntimeEngine.class),
                mock(MetadataHotReloadManager.class),
                mock(CodeGenerator.class),
                mock(PermissionChecker.class),
                mock(TenantContext.class),
                mock(CacheProvider.class),
                mock(AuditLogger.class),
                orchestrator
        );
    }
}
