package com.aurora.core.infrastructure.ai;

import com.aurora.core.infrastructure.ai.LlmProviderRouter.ProviderConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * LLM Provider Router Auto-Configuration
 *
 * Wires up the LLMRouter with primary and fallback ChatModel beans.
 * Enabled by default. Disable via:
 *   aurora.ai-routing.enabled=false
 *
 * Supports External Secrets Operator (ESO) or HashiCorp Vault for
 * API key injection — keys are read from environment variables
 * which ESO/Vault populate via Kubernetes Secrets.
 */
@Configuration
@ConditionalOnProperty(prefix = "aurora.ai-routing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LlmRoutingAutoConfiguration {

    @Value("${spring.ai.routing.primary:anthropic}")
    private String primaryProvider;

    @Value("${spring.ai.routing.fallback:openai}")
    private String fallbackProvider;

    @Value("${spring.ai.routing.timeout:15s}")
    private Duration routingTimeout;

    @Value("${aurora.skill.fallback.max-retries:3}")
    private int maxRetries;

    @Value("${aurora.skill.fallback.retry-delay:1s}")
    private Duration retryDelay;

    /**
     * Create LLM Router if both primary and fallback models are available.
     * Models are provided by Spring AI auto-configuration when API keys are set.
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public LlmProviderRouter llmProviderRouter(Map<String, ChatModel> chatModels) {
        // Spring AI registers ChatModel beans with names like "anthropicChatModel", "openAiChatModel"
        Map<String, ChatModel> providers = new HashMap<>();

        // Map provider aliases to actual bean names
        for (Map.Entry<String, ChatModel> entry : chatModels.entrySet()) {
            String beanName = entry.getKey().toLowerCase();
            if (beanName.contains("anthropic")) {
                providers.put("anthropic", entry.getValue());
            } else if (beanName.contains("openai") || beanName.contains("open")) {
                providers.put("openai", entry.getValue());
            }
        }

        if (providers.isEmpty()) {
            throw new IllegalStateException(
                "No LLM providers configured. Set ANTHROPIC_API_KEY and/or OPENAI_API_KEY.");
        }

        ProviderConfig config = new ProviderConfig(
            primaryProvider,
            fallbackProvider,
            routingTimeout,
            maxRetries,
            retryDelay
        );

        return new LlmProviderRouter(providers, config);
    }
}
