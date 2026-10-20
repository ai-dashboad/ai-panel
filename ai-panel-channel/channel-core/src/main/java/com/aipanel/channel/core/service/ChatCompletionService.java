package com.aipanel.channel.core.service;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.router.ModelRouterService;
import com.aipanel.channel.core.token.TokenCounterService;
import com.aipanel.channel.core.upstream.ClientDisconnectedException;
import com.aipanel.channel.core.upstream.StreamResult;
import com.aipanel.channel.core.upstream.StreamSink;
import com.aipanel.channel.data.mysql.mapper.UsageLogMapper;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.data.mysql.po.UsageLogPO;
import com.aipanel.channel.data.redis.ApiKeyCacheService;
import com.aipanel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * Validate the channel API key, rate limit, model allow-list, routability and quota.
     * Everything that can fail before talking to an upstream happens here, so streaming
     * responses can still return a proper HTTP error status.
     */
    public ChannelApiKeyPO authorize(String authorization, ChatCompletionRequest request) {
        ChannelApiKeyPO key;
        try {
            key = apiKeyService.validateKey(authorization);
        } catch (BusinessException e) {
            String msg = e.getMessage() == null ? "Invalid API key" : e.getMessage();
            if (msg.contains("quota")) {
                throw new ChannelApiException(429, "insufficient_quota", "insufficient_quota", msg);
            }
            throw new ChannelApiException(401, "invalid_request_error", "invalid_api_key", msg);
        }

        if (!cacheService.checkRateLimit(key.getKeyId(), key.getRateLimit())) {
            throw new ChannelApiException(429, "rate_limit_error", "rate_limit_exceeded", "Rate limit exceeded");
        }

        if (key.getAllowedModels() != null && !key.getAllowedModels().isEmpty()) {
            List<String> allowedModels = Arrays.asList(key.getAllowedModels().split(","));
            if (!allowedModels.contains(request.getModel())) {
                throw new ChannelApiException(403, "invalid_request_error", "model_not_allowed",
                    "Model not allowed for this API key: " + request.getModel());
            }
        }

        if (!modelRouter.isModelSupported(request.getModel())) {
            throw new ChannelApiException(404, "invalid_request_error", "model_not_found",
                "The model '" + request.getModel() + "' is not configured on any enabled provider");
        }

        int estimatedPromptTokens = tokenCounter.countMessages(request.getMessages());
        if (!apiKeyService.hasQuota(key.getKeyId(), estimatedPromptTokens)) {
            throw new ChannelApiException(429, "insufficient_quota", "insufficient_quota", "Insufficient quota");
        }
        return key;
    }

    /**
     * Process chat completion request
     */
    public ChatCompletionResponse chatCompletion(String authorization, ChatCompletionRequest request) {
        ChannelApiKeyPO key = authorize(authorization, request);
        return chatCompletion(key, request);
    }

    /**
     * Non-streaming completion for an already authorized key
     */
    public ChatCompletionResponse chatCompletion(ChannelApiKeyPO key, ChatCompletionRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        try {
            ChatCompletionResponse response = modelRouter.chatCompletion(request);
            ChatCompletionResponse.Usage usage = response.getUsage();
            if (usage == null || usage.getTotalTokens() == null) {
                usage = estimateUsage(request, completionText(response));
            }
            apiKeyService.consumeQuota(key.getKeyId(), usage.getTotalTokens());
            logUsage(key, requestId, request.getModel(), response.getModel(), usage, startTime, null);
            return response;
        } catch (RuntimeException e) {
            logUsage(key, requestId, request.getModel(), null, null, startTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Streaming completion for an already authorized key. Chunks are written to {@code sink}.
     */
    public StreamResult streamChatCompletion(ChannelApiKeyPO key, ChatCompletionRequest request, StreamSink sink) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        try {
            StreamResult result = modelRouter.streamChatCompletion(request, sink);
            ChatCompletionResponse.Usage usage = result.getUsage();
            if (usage == null || usage.getTotalTokens() == null) {
                usage = estimateUsage(request, result.getGeneratedText().toString());
            }
            apiKeyService.consumeQuota(key.getKeyId(), usage.getTotalTokens());
            logUsage(key, requestId, request.getModel(), result.getModel(), usage, startTime, null);
            return result;
        } catch (ClientDisconnectedException e) {
            logUsage(key, requestId, request.getModel(), null, null, startTime, "client disconnected");
            throw e;
        } catch (RuntimeException e) {
            logUsage(key, requestId, request.getModel(), null, null, startTime, e.getMessage());
            throw e;
        }
    }

    private static String completionText(ChatCompletionResponse response) {
        StringBuilder sb = new StringBuilder();
        if (response.getChoices() != null) {
            for (ChatCompletionResponse.Choice c : response.getChoices()) {
                if (c.getMessage() == null) {
                    continue;
                }
                if (c.getMessage().getContent() != null) {
                    sb.append(c.getMessage().getContent());
                }
                if (c.getMessage().getToolCalls() != null) {
                    c.getMessage().getToolCalls().forEach(tc -> {
                        if (tc.getFunction() != null) {
                            sb.append(tc.getFunction().getName()).append(tc.getFunction().getArguments());
                        }
                    });
                }
            }
        }
        return sb.toString();
    }

    /** Estimate usage when the upstream did not report any (common for self-hosted streams). */
    private ChatCompletionResponse.Usage estimateUsage(ChatCompletionRequest request, String completion) {
        int prompt = tokenCounter.countMessages(request.getMessages());
        int completionTokens = tokenCounter.countTokens(completion == null ? "" : completion);
        return ChatCompletionResponse.Usage.builder()
            .promptTokens(prompt).completionTokens(completionTokens).totalTokens(prompt + completionTokens).build();
    }

    /**
     * Get list of available models
     */
    public ModelListResponse listModels(String authorization) {
        ChannelApiKeyPO key;
        try {
            key = apiKeyService.validateKey(authorization);
        } catch (BusinessException e) {
            throw new ChannelApiException(401, "invalid_request_error", "invalid_api_key",
                e.getMessage() == null ? "Invalid API key" : e.getMessage());
        }

        ModelListResponse response = modelRouter.listModels();

        if (key.getAllowedModels() != null && !key.getAllowedModels().isEmpty()) {
            List<String> allowedModels = Arrays.asList(key.getAllowedModels().split(","));
            response.setData(response.getData().stream()
                .filter(m -> allowedModels.contains(m.getId()))
                .toList());
        }

        return response;
    }

    private void logUsage(ChannelApiKeyPO key, String requestId, String model, String actualModel,
                          ChatCompletionResponse.Usage usage, long startTime, String errorMessage) {
        try {
            UsageLogPO log = new UsageLogPO();
            log.setChannelId(key.getChannelId());
            log.setKeyId(key.getKeyId());
            log.setRequestId(requestId);
            log.setModel(model);
            log.setLatencyMs(System.currentTimeMillis() - startTime);

            if (usage != null) {
                log.setStatus("success");
                log.setPromptTokens(usage.getPromptTokens());
                log.setCompletionTokens(usage.getCompletionTokens());
                log.setTotalTokens(usage.getTotalTokens());
                log.setActualModel(actualModel != null ? actualModel : model);
                // Calculate cost (simplified - in production, use actual pricing)
                log.setCost(new BigDecimal(usage.getTotalTokens()).multiply(new BigDecimal("0.00001")));
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
