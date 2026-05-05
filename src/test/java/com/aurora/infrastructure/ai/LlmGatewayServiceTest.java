package com.aurora.infrastructure.ai;

import com.aurora.core.ai.SkillTelemetry;
import com.aurora.core.infrastructure.ai.LlmGatewayService;
import com.aurora.core.infrastructure.ai.LlmProviderRouter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LlmGatewayService}.
 */
@ExtendWith(MockitoExtension.class)
class LlmGatewayServiceTest {

    @Mock
    private LlmProviderRouter router;

    private SkillTelemetry telemetry;
    private LlmGatewayService gateway;

    @BeforeEach
    void setUp() {
        telemetry = new SkillTelemetry(new SimpleMeterRegistry(), 0.003, 0.015);
        gateway = new LlmGatewayService(router, telemetry);
    }

    @Nested
    @DisplayName("Cost Estimation")
    class CostEstimation {

        @Test
        @DisplayName("should estimate Anthropic cost correctly")
        void shouldEstimateAnthropicCost() {
            double cost = gateway.costEstimation("anthropic", 1_000_000, 500_000);
            // $3/M input + $15/M output = $3 + $7.5 = $10.5
            assertEquals(10.5, cost, 0.01);
        }

        @Test
        @DisplayName("should estimate OpenAI cost correctly")
        void shouldEstimateOpenAICost() {
            double cost = gateway.costEstimation("openai", 1_000_000, 500_000);
            // $2.5/M input + $10/M output = $2.5 + $5 = $7.5
            assertEquals(7.5, cost, 0.01);
        }

        @Test
        @DisplayName("should return zero cost for zero tokens")
        void shouldReturnZeroForZeroTokens() {
            assertEquals(0.0, gateway.costEstimation("anthropic", 0, 0));
        }

        @Test
        @DisplayName("should handle small token counts")
        void shouldHandleSmallTokenCounts() {
            double cost = gateway.costEstimation("anthropic", 100, 50);
            assertTrue(cost > 0);
            assertTrue(cost < 0.01); // Less than 1 cent
        }
    }

    @Nested
    @DisplayName("Circuit State")
    class CircuitState {

        @Test
        @DisplayName("should report circuit state from router")
        void shouldReportCircuitState() {
            when(router.getCircuitState()).thenReturn(
                    new LlmProviderRouter.CircuitState(
                            "anthropic", false, 0, null, 10, 1, 0, "healthy"));

            var state = gateway.getCircuitState();
            assertEquals("anthropic", state.currentProvider());
            assertEquals("healthy", state.healthStatus());
        }
    }
}
