package com.aipanel.channel.core.service;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.router.ModelRouterService;
import com.aipanel.channel.core.token.TokenCounterService;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.data.mysql.po.UsageLogPO;
import com.aipanel.channel.data.mysql.mapper.UsageLogMapper;
import com.aipanel.channel.data.redis.ApiKeyCacheService;
import com.aipanel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Chat completion service with authentication, rate limiting, and usage tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCompletionService {

    private final ApiKeyGeneratorService apiKeyService;
    private final ModelRouterService modelRouter;
    private final TokenCounterService tokenCounter;
    private final ApiKeyCacheService cacheService;
    private final UsageLogMapper usageLogMapper;

    /**
     * Process chat completion request
     */
    public ChatCompletionResponse chatCompletion(String authorization, ChatCompletionRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // Validate API key
        ChannelApiKeyPO key = apiKeyService.validateKey(authorization);

        // Check rate limit
        if (!cacheService.checkRateLimit(key.getKeyId(), key.getRateLimit())) {
            throw new BusinessException("Rate limit exceeded");
        }

        // Check if model is allowed
        if (key.getAllowedModels() != null && !key.getAllowedModels().isEmpty()) {
            List<String> allowedModels = Arrays.asList(key.getAllowedModels().split(","));
            if (!allowedModels.contains(request.getModel())) {
                throw new BusinessException("Model not allowed for this API key: " + request.getModel());
            }
        }

        // Check if model is supported
        if (!modelRouter.isModelSupported(request.getModel())) {
            throw new BusinessException("Unsupported model: " + request.getModel());
        }

        // Estimate prompt tokens for quota check
        int estimatedPromptTokens = tokenCounter.countMessages(request.getMessages());
        if (!apiKeyService.hasQuota(key.getKeyId(), estimatedPromptTokens)) {
            throw new BusinessException("Insufficient quota");
        }

        try {
            // Route to provider
            ChatCompletionResponse response = modelRouter.chatCompletion(request);

            // Record usage
            int totalTokens = response.getUsage().getTotalTokens();
            apiKeyService.consumeQuota(key.getKeyId(), totalTokens);

            // Log usage
            logUsage(key, requestId, request.getModel(), response, startTime, null);

            return response;

        } catch (Exception e) {
            // Log failed request
            logUsage(key, requestId, request.getModel(), null, startTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Process streaming chat completion request
     */
    public SseEmitter streamChatCompletion(String authorization, ChatCompletionRequest request) {
        // Validate API key
        ChannelApiKeyPO key = apiKeyService.validateKey(authorization);

        // Check rate limit
        if (!cacheService.checkRateLimit(key.getKeyId(), key.getRateLimit())) {
            throw new BusinessException("Rate limit exceeded");
        }

        // Check if model is allowed
        if (key.getAllowedModels() != null && !key.getAllowedModels().isEmpty()) {
            List<String> allowedModels = Arrays.asList(key.getAllowedModels().split(","));
            if (!allowedModels.contains(request.getModel())) {
                throw new BusinessException("Model not allowed for this API key: " + request.getModel());
            }
        }

        // Check if model is supported
        if (!modelRouter.isModelSupported(request.getModel())) {
            throw new BusinessException("Unsupported model: " + request.getModel());
        }

        // Route to provider for streaming
        return modelRouter.streamChatCompletion(request);
    }

    /**
     * Get list of available models
     */
    public ModelListResponse listModels(String authorization) {
        // Validate API key
        ChannelApiKeyPO key = apiKeyService.validateKey(authorization);

        ModelListResponse response = modelRouter.listModels();

        // Filter by allowed models if specified
        if (key.getAllowedModels() != null && !key.getAllowedModels().isEmpty()) {
            List<String> allowedModels = Arrays.asList(key.getAllowedModels().split(","));
            response.setData(response.getData().stream()
                .filter(m -> allowedModels.contains(m.getId()))
                .toList());
        }

        return response;
    }

    private void logUsage(ChannelApiKeyPO key, String requestId, String model,
                          ChatCompletionResponse response, long startTime, String errorMessage) {
        try {
            UsageLogPO log = new UsageLogPO();
            log.setChannelId(key.getChannelId());
            log.setKeyId(key.getKeyId());
            log.setRequestId(requestId);
            log.setModel(model);
            log.setLatencyMs(System.currentTimeMillis() - startTime);

            if (response != null) {
                log.setStatus("success");
                log.setPromptTokens(response.getUsage().getPromptTokens());
                log.setCompletionTokens(response.getUsage().getCompletionTokens());
                log.setTotalTokens(response.getUsage().getTotalTokens());
                log.setActualModel(response.getModel());
                // Calculate cost (simplified - in production, use actual pricing)
                log.setCost(new BigDecimal(response.getUsage().getTotalTokens())
                    .multiply(new BigDecimal("0.00001")));
            } else {
                log.setStatus("error");
                log.setErrorMessage(errorMessage);
            }

            log.setCreatedAt(LocalDateTime.now());
            usageLogMapper.insert(log);

        } catch (Exception e) {
            log.error("Failed to log usage", e);
        }
    }
}
