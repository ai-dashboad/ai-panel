package com.aipanel.channel.core.router.impl;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.pool.ApiKeyPool;
import com.aipanel.channel.core.pool.DynamicProviderFactory;
import com.aipanel.channel.core.router.ModelRouterService;
import com.aipanel.channel.core.router.provider.AiProvider;
import com.aipanel.channel.core.router.provider.ProviderRegistry;
import com.aipanel.channel.data.mysql.mapper.ProviderModelMapper;
import com.aipanel.channel.data.mysql.po.ProviderModelPO;
import com.aipanel.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model router service implementation with fallback support
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouterServiceImpl implements ModelRouterService {

    private final ProviderRegistry providerRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final DynamicProviderFactory dynamicProviderFactory;
    private final ApiKeyPool apiKeyPool;
    private final ProviderModelMapper providerModelMapper;

    // Model to provider mapping with fallback chains
    private static final Map<String, List<String>> MODEL_FALLBACK_CHAINS = new ConcurrentHashMap<>();

    static {
        // GPT models fallback to DeepSeek
        MODEL_FALLBACK_CHAINS.put("gpt-4", List.of("openai", "anthropic", "deepseek"));
        MODEL_FALLBACK_CHAINS.put("gpt-4-turbo", List.of("openai", "anthropic", "deepseek"));
        MODEL_FALLBACK_CHAINS.put("gpt-4o", List.of("openai", "anthropic", "deepseek"));
        MODEL_FALLBACK_CHAINS.put("gpt-3.5-turbo", List.of("openai", "deepseek"));

        // Claude models
        MODEL_FALLBACK_CHAINS.put("claude-3-opus", List.of("anthropic", "openai"));
        MODEL_FALLBACK_CHAINS.put("claude-3-sonnet", List.of("anthropic", "openai"));
        MODEL_FALLBACK_CHAINS.put("claude-3-haiku", List.of("anthropic", "deepseek"));

        // DeepSeek models
        MODEL_FALLBACK_CHAINS.put("deepseek-chat", List.of("deepseek", "openai"));
        MODEL_FALLBACK_CHAINS.put("deepseek-coder", List.of("deepseek", "openai"));
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        String model = request.getModel();

        // First, try dynamic provider factory (uses database-configured keys)
        try {
            if (hasConfiguredKeys(model)) {
                log.info("Using dynamic provider factory for model: {}", model);
                return dynamicProviderFactory.chatCompletion(request);
            }
        } catch (Exception e) {
            log.warn("Dynamic provider failed, falling back to static providers: {}", e.getMessage());
        }

        // Fallback to static provider configuration
        List<String> providerChain = getProviderChain(model);

        Exception lastException = null;
        for (String providerName : providerChain) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerName);

            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                log.warn("Circuit breaker open for provider: {}", providerName);
                continue;
            }

            try {
                AiProvider provider = providerRegistry.getProvider(providerName);
                if (provider == null) {
                    log.warn("Provider not found: {}", providerName);
                    continue;
                }

                log.info("Routing request to provider: {} for model: {}", providerName, model);
                ChatCompletionResponse response = circuitBreaker.executeSupplier(
                    () -> provider.chatCompletion(request)
                );

                // Add metadata
                response.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
                response.setCreated(Instant.now().getEpochSecond());
                response.setObject("chat.completion");

                return response;

            } catch (Exception e) {
                log.error("Provider {} failed for model {}: {}", providerName, model, e.getMessage());
                lastException = e;
            }
        }

        throw new BusinessException("All providers failed: " +
            (lastException != null ? lastException.getMessage() : "No providers available"));
    }

    /**
     * Check if there are configured API keys for the model's provider
     */
    private boolean hasConfiguredKeys(String model) {
        // Determine provider type from model
        String providerType = inferProviderFromModel(model);
        if (providerType == null) {
            return false;
        }
        return apiKeyPool.hasAvailableKeys(providerType);
    }

    private String inferProviderFromModel(String model) {
        String lowerModel = model.toLowerCase();
        if (lowerModel.startsWith("gpt") || lowerModel.contains("openai")) {
            return "openai";
        } else if (lowerModel.startsWith("claude") || lowerModel.contains("anthropic")) {
            return "anthropic";
        } else if (lowerModel.startsWith("deepseek")) {
            return "deepseek";
        } else if (lowerModel.startsWith("moonshot")) {
            return "moonshot";
        }
        return null;
    }

    @Override
    public SseEmitter streamChatCompletion(ChatCompletionRequest request) {
        String model = request.getModel();
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout

        // First, try dynamic provider factory (uses database-configured keys)
        try {
            if (hasConfiguredKeys(model)) {
                log.info("Using dynamic provider factory for streaming model: {}", model);
                dynamicProviderFactory.streamChatCompletion(request, emitter);
                return emitter;
            }
        } catch (Exception e) {
            log.warn("Dynamic provider failed for streaming, falling back: {}", e.getMessage());
        }

        // Fallback to static provider configuration
        List<String> providerChain = getProviderChain(model);

        for (String providerName : providerChain) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerName);

            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                continue;
            }

            try {
                AiProvider provider = providerRegistry.getProvider(providerName);
                if (provider == null) {
                    continue;
                }

                log.info("Streaming request to provider: {} for model: {}", providerName, model);
                provider.streamChatCompletion(request, emitter);
                return emitter;

            } catch (Exception e) {
                log.error("Provider {} failed for streaming: {}", providerName, e.getMessage());
            }
        }

        emitter.completeWithError(new BusinessException("All providers failed"));
        return emitter;
    }

    @Override
    public ModelListResponse listModels() {
        List<ModelListResponse.ModelData> models = new ArrayList<>();
        long created = Instant.now().getEpochSecond();
        java.util.Set<String> addedModels = new java.util.HashSet<>();

        // First, add models from database configuration (if keys are available)
        List<ProviderModelPO> dbModels = providerModelMapper.selectList(
            new LambdaQueryWrapper<ProviderModelPO>()
                .eq(ProviderModelPO::getEnabled, true)
        );

        for (ProviderModelPO modelPO : dbModels) {
            // Only add if provider has available keys
            String providerType = inferProviderFromModelId(modelPO.getModelId());
            if (providerType != null && apiKeyPool.hasAvailableKeys(providerType)) {
                models.add(ModelListResponse.ModelData.builder()
                    .id(modelPO.getModelId())
                    .object("model")
                    .created(created)
                    .ownedBy(providerType)
                    .build());
                addedModels.add(modelPO.getModelId());
            }
        }

        // Add static fallback chain models
        for (String model : MODEL_FALLBACK_CHAINS.keySet()) {
            if (!addedModels.contains(model)) {
                models.add(ModelListResponse.ModelData.builder()
                    .id(model)
                    .object("model")
                    .created(created)
                    .ownedBy("ai-panel")
                    .build());
                addedModels.add(model);
            }
        }

        // Add additional models from registered static providers
        for (AiProvider provider : providerRegistry.getAllProviders()) {
            for (String model : provider.getSupportedModels()) {
                if (!addedModels.contains(model)) {
                    models.add(ModelListResponse.ModelData.builder()
                        .id(model)
                        .object("model")
                        .created(created)
                        .ownedBy(provider.getName())
                        .build());
                    addedModels.add(model);
                }
            }
        }

        return ModelListResponse.builder()
            .object("list")
            .data(models)
            .build();
    }

    private String inferProviderFromModelId(String modelId) {
        String lower = modelId.toLowerCase();
        if (lower.startsWith("gpt") || lower.contains("o1-")) {
            return "openai";
        } else if (lower.startsWith("claude")) {
            return "anthropic";
        } else if (lower.startsWith("deepseek")) {
            return "deepseek";
        } else if (lower.startsWith("moonshot")) {
            return "moonshot";
        }
        return null;
    }

    @Override
    public List<String> getProviderModels(String provider) {
        AiProvider aiProvider = providerRegistry.getProvider(provider);
        if (aiProvider == null) {
            throw new BusinessException("Provider not found: " + provider);
        }
        return aiProvider.getSupportedModels();
    }

    @Override
    public boolean isModelSupported(String model) {
        if (MODEL_FALLBACK_CHAINS.containsKey(model)) {
            return true;
        }
        for (AiProvider provider : providerRegistry.getAllProviders()) {
            if (provider.getSupportedModels().contains(model)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getProviderChain(String model) {
        List<String> chain = MODEL_FALLBACK_CHAINS.get(model);
        if (chain != null) {
            return chain;
        }

        // Determine provider from model name
        if (model.startsWith("gpt-")) {
            return List.of("openai", "deepseek");
        } else if (model.startsWith("claude-")) {
            return List.of("anthropic", "openai");
        } else if (model.startsWith("deepseek-")) {
            return List.of("deepseek", "openai");
        } else if (model.startsWith("gemini-")) {
            return List.of("google", "openai");
        } else if (model.startsWith("moonshot-")) {
            return List.of("moonshot", "deepseek");
        }

        // Default fallback
        return List.of("openai", "anthropic", "deepseek");
    }
}
