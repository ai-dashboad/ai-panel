package com.aipanel.dispatch.core.client;

import com.aipanel.dispatch.api.dto.request.ChatRequest;
import com.aipanel.dispatch.api.dto.response.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI Provider Client Interface
 * Implementations for each AI provider (OpenAI, Anthropic, Google, etc.)
 */
public interface AiClient {

    /**
     * Get provider code
     *
     * @return Provider code (e.g., "openai", "anthropic")
     */
    String getProviderCode();

    /**
     * Check if client supports the given model
     *
     * @param model Model ID
     * @return true if supported
     */
    boolean supportsModel(String model);

    /**
     * Send synchronous chat request
     *
     * @param model Actual model ID to use
     * @param request Chat request
     * @return Chat response
     */
    Mono<ChatResponse> chat(String model, ChatRequest request);

    /**
     * Send streaming chat request
     *
     * @param model Actual model ID to use
     * @param request Chat request
     * @return Flux of chat response chunks
     */
    Flux<ChatResponse> chatStream(String model, ChatRequest request);

    /**
     * Health check for the provider
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Get current health status
     *
     * @return Health status
     */
    default HealthStatus getHealthStatus() {
        return isHealthy() ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
    }

    enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }
}
