package com.aurora.core.infrastructure.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Map;

@Configuration
@Profile("dev")
public class DevLlmRouterConfiguration {

    @Bean
    @ConditionalOnMissingBean(LlmProviderRouter.class)
    public LlmProviderRouter devLlmProviderRouter() {
        return new LlmProviderRouter(
                Map.of(),
                new LlmProviderRouter.ProviderConfig("anthropic", "openai", Duration.ofSeconds(5), 1, Duration.ofSeconds(5))
        );
    }
}
