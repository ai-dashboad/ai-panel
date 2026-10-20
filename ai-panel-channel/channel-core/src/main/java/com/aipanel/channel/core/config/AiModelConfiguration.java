package com.aipanel.channel.core.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual configuration for AI models
 * Only creates beans when API keys are actually provided
 */
@Configuration
public class AiModelConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key", matchIfMissing = false)
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl) {

        // Skip if placeholder or empty
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk-placeholder")) {
            return null;
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.anthropic", name = "api-key", matchIfMissing = false)
    public AnthropicChatModel anthropicChatModel(
            @Value("${spring.ai.anthropic.api-key}") String apiKey) {

        // Skip if placeholder or empty
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk-placeholder")) {
            return null;
        }

        AnthropicApi anthropicApi = new AnthropicApi(apiKey);

        return AnthropicChatModel.builder()
            .anthropicApi(anthropicApi)
            .build();
    }
}
