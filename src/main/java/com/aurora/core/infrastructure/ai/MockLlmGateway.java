package com.aurora.core.infrastructure.ai;

import com.aurora.core.ai.SkillTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock LLM Gateway for test environments.
 *
 * <p>Returns fixed JSON responses without calling any external API.
 * Activated via {@code spring.profiles.active=test} or
 * {@code aurora.llm.mock=true}.
 *
 * <p>Response format matches the expected JSON structure for the
 * self-correction validation pipeline.
 */
@Primary
@Component
@Profile("test")
@ConditionalOnProperty(name = "aurora.llm.mock", havingValue = "true", matchIfMissing = false)
public class MockLlmGateway extends LlmGatewayService {

    private static final Logger log = LoggerFactory.getLogger(MockLlmGateway.class);

    private static final String MOCK_RESPONSE = """
            {
                "skillId": "mock_skill",
                "version": "1.0.0",
                "status": "generated",
                "files": [
                    {
                        "name": "Entity.java",
                        "type": "java",
                        "content": "package com.aurora.generated;\\n@Entity\\npublic class Entity {}"
                    }
                ],
                "metadata": {
                    "tokensUsed": 150,
                    "costUsd": 0.001,
                    "latencyMs": 50
                }
            }
            """;

    protected MockLlmGateway(LlmProviderRouter router,
                              SkillTelemetry telemetry) {
        super(router, telemetry);
    }

    @Override
    public String call(String prompt) {
        log.debug("Mock LLM call: prompt length={}", prompt.length());
        return MOCK_RESPONSE;
    }

    @Override
    public String retryWithBackoff(String prompt, int maxRetries) {
        log.debug("Mock LLM retryWithBackoff: prompt length={}, maxRetries={}",
                prompt.length(), maxRetries);
        return MOCK_RESPONSE;
    }
}
