package com.aipanel.dispatch.core.service;

import com.aipanel.common.exception.ModelException;
import com.aipanel.dispatch.api.dto.request.ChatRequest;
import com.aipanel.dispatch.api.dto.response.ChatResponse;
import com.aipanel.dispatch.core.client.AiClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model Router Service
 * Routes requests to appropriate AI provider with fallback support
 */
@Service
@Slf4j
public class ModelRouterService {

    private final List<AiClient> aiClients;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, List<String>> fallbackChains;

    public ModelRouterService(List<AiClient> aiClients) {
        this.aiClients = aiClients;

        // Initialize circuit breaker registry
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(config);

        // Initialize fallback chains
        this.fallbackChains = new ConcurrentHashMap<>();
        initFallbackChains();

        log.info("ModelRouterService initialized with {} AI clients", aiClients.size());
    }

    private void initFallbackChains() {
        // GPT-4 fallback chain
        fallbackChains.put("gpt-4", List.of("gpt-4", "claude-3-opus-20240229", "gpt-4-turbo", "gpt-3.5-turbo"));
        fallbackChains.put("gpt-4o", List.of("gpt-4o", "gpt-4-turbo", "claude-3-5-sonnet-20241022", "gpt-3.5-turbo"));

        // Claude fallback chain
        fallbackChains.put("claude-3-opus", List.of("claude-3-opus-20240229", "gpt-4", "claude-3-sonnet-20240229"));
        fallbackChains.put("claude-3-5-sonnet", List.of("claude-3-5-sonnet-20241022", "gpt-4o", "claude-3-sonnet-20240229"));

        // Default fallback
        fallbackChains.put("default", List.of("gpt-3.5-turbo", "claude-3-haiku-20240307"));
    }

    /**
     * Route chat request to appropriate AI provider
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        String requestedModel = request.getModel();
        boolean allowFallback = request.getAllowFallback() != null && request.getAllowFallback();

        // Get fallback chain
        List<String> modelsToTry = allowFallback ?
            getFallbackChain(requestedModel) :
            List.of(requestedModel);

        return tryModels(modelsToTry, request);
    }

    /**
     * Route streaming chat request
     */
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        String model = request.getModel();
        AiClient client = findClientForModel(model);

        if (client == null) {
            return Flux.error(ModelException.notFound(model));
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
            client.getProviderCode() + ":" + model);

        return client.chatStream(model, request)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .doOnSubscribe(s -> log.info("Streaming request to model={}, provider={}",
                model, client.getProviderCode()));
    }

    private Mono<ChatResponse> tryModels(List<String> models, ChatRequest request) {
        if (models.isEmpty()) {
            return Mono.error(ModelException.allUnavailable());
        }

        String model = models.get(0);
        List<String> remainingModels = models.subList(1, models.size());

        AiClient client = findClientForModel(model);
        if (client == null) {
            log.warn("No client found for model={}, trying next", model);
            return tryModels(remainingModels, request);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
            client.getProviderCode() + ":" + model);

        log.info("Routing request to model={}, provider={}", model, client.getProviderCode());

        return client.chat(model, request)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .doOnSuccess(r -> log.info("Request succeeded: model={}", model))
            .onErrorResume(e -> {
                log.warn("Request failed for model={}: {}, trying fallback", model, e.getMessage());
                if (remainingModels.isEmpty()) {
                    return Mono.error(ModelException.providerError(model, client.getProviderCode(), e));
                }
                return tryModels(remainingModels, request);
            });
    }

    private AiClient findClientForModel(String model) {
        for (AiClient client : aiClients) {
            if (client.supportsModel(model) && client.isHealthy()) {
                return client;
            }
        }

        // Try unhealthy clients as last resort
        for (AiClient client : aiClients) {
            if (client.supportsModel(model)) {
                return client;
            }
        }

        return null;
    }

    private List<String> getFallbackChain(String model) {
        // Check exact match
        if (fallbackChains.containsKey(model)) {
            return fallbackChains.get(model);
        }

        // Check prefix match
        for (String key : fallbackChains.keySet()) {
            if (model.toLowerCase().startsWith(key.toLowerCase())) {
                return fallbackChains.get(key);
            }
        }

        // Return model with default fallback
        List<String> chain = new java.util.ArrayList<>();
        chain.add(model);
        chain.addAll(fallbackChains.getOrDefault("default", List.of()));
        return chain;
    }

    /**
     * Get health status of all providers
     */
    public Map<String, Boolean> getProviderHealth() {
        Map<String, Boolean> health = new ConcurrentHashMap<>();
        for (AiClient client : aiClients) {
            health.put(client.getProviderCode(), client.isHealthy());
        }
        return health;
    }
}
