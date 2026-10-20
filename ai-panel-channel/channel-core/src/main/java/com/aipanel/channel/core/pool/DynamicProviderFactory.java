package com.aipanel.channel.core.pool;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.data.mysql.mapper.ProviderModelMapper;
import com.aipanel.channel.data.mysql.po.ProviderModelPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Provider Factory - creates AI providers dynamically using API Key Pool
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicProviderFactory {

    private final ApiKeyPool apiKeyPool;
    private final ProviderModelMapper providerModelMapper;

    /**
     * Cache for model -> provider type mapping
     */
    private final Map<String, String> modelProviderCache = new ConcurrentHashMap<>();

    /**
     * Execute chat completion with automatic key selection
     */
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        String model = request.getModel();
        String providerType = resolveProviderType(model);

        if (providerType == null) {
            throw new IllegalArgumentException("Unknown model: " + model);
        }

        ApiKeyPool.KeyEntry keyEntry = apiKeyPool.getNextKey(providerType);
        if (keyEntry == null) {
            throw new IllegalStateException("No available API keys for provider: " + providerType);
        }

        ApiKeyPool.ProviderConfig config = apiKeyPool.getProviderConfig(providerType);

        try {
            ChatCompletionResponse response = executeCompletion(providerType, config, keyEntry, request);

            // Record success
            long tokensUsed = response.getUsage() != null ?
                response.getUsage().getTotalTokens() : 0;
            apiKeyPool.recordSuccess(keyEntry.getId(), tokensUsed);

            return response;

        } catch (Exception e) {
            // Record error and potentially retry with another key
            apiKeyPool.recordError(keyEntry.getId(), e.getMessage());

            // Try with another key
            ApiKeyPool.KeyEntry retryKey = apiKeyPool.getNextKey(providerType);
            if (retryKey != null && !retryKey.getId().equals(keyEntry.getId())) {
                log.info("Retrying with different key for provider: {}", providerType);
                try {
                    return executeCompletion(providerType, config, retryKey, request);
                } catch (Exception retryError) {
                    apiKeyPool.recordError(retryKey.getId(), retryError.getMessage());
                    throw retryError;
                }
            }

            throw e;
        }
    }

    /**
     * Execute streaming chat completion
     */
    public void streamChatCompletion(ChatCompletionRequest request, SseEmitter emitter) {
        String model = request.getModel();
        String providerType = resolveProviderType(model);

        if (providerType == null) {
            throw new IllegalArgumentException("Unknown model: " + model);
        }

        ApiKeyPool.KeyEntry keyEntry = apiKeyPool.getNextKey(providerType);
        if (keyEntry == null) {
            throw new IllegalStateException("No available API keys for provider: " + providerType);
        }

        ApiKeyPool.ProviderConfig config = apiKeyPool.getProviderConfig(providerType);

        executeStreamCompletion(providerType, config, keyEntry, request, emitter);
    }

    private String resolveProviderType(String model) {
        // Check cache first
        String cached = modelProviderCache.get(model);
        if (cached != null) {
            return cached;
        }

        // Query database for model -> provider mapping
        List<ProviderModelPO> models = providerModelMapper.selectList(
            new LambdaQueryWrapper<ProviderModelPO>()
                .eq(ProviderModelPO::getModelId, model)
                .eq(ProviderModelPO::getEnabled, true)
        );

        if (!models.isEmpty()) {
            // Get provider type from provider_config
            ApiKeyPool.ProviderConfig config = apiKeyPool.getProviderConfig(
                getProviderTypeById(models.get(0).getProviderId())
            );
            if (config != null) {
                modelProviderCache.put(model, config.getType());
                return config.getType();
            }
        }

        // Fallback: infer from model name
        String providerType = inferProviderFromModel(model);
        if (providerType != null) {
            modelProviderCache.put(model, providerType);
        }
        return providerType;
    }

    private String getProviderTypeById(Long providerId) {
        for (String providerType : apiKeyPool.getAvailableProviders()) {
            ApiKeyPool.ProviderConfig config = apiKeyPool.getProviderConfig(providerType);
            if (config != null && config.getId().equals(providerId)) {
                return providerType;
            }
        }
        return null;
    }

    private String inferProviderFromModel(String model) {
        String lowerModel = model.toLowerCase();
        if (lowerModel.startsWith("gpt") || lowerModel.startsWith("o1") || lowerModel.startsWith("o3")
                || lowerModel.contains("openai")) {
            return "openai";
        } else if (lowerModel.startsWith("claude") || lowerModel.contains("anthropic")) {
            return "anthropic";
        } else if (lowerModel.startsWith("deepseek") || lowerModel.contains("deepseek")) {
            return "deepseek";
        } else if (lowerModel.startsWith("moonshot") || lowerModel.startsWith("kimi")) {
            return "moonshot";
        } else if (lowerModel.startsWith("glm") || lowerModel.contains("zhipu")) {
            return "zhipu";
        } else if (lowerModel.startsWith("qwen")) {
            return "qwen";
        } else if (lowerModel.startsWith("gemini")) {
            return "google";
        } else if (lowerModel.startsWith("minimax") || lowerModel.startsWith("abab")) {
            return "minimax";
        } else if (lowerModel.startsWith("step-")) {
            return "stepfun";
        } else if (lowerModel.startsWith("doubao")) {
            return "doubao";
        } else if (lowerModel.contains("openrouter") || lowerModel.contains("/")) {
            return "openrouter";
        }
        return "openai";
    }

    private boolean isOpenAiCompatible(String providerType) {
        return switch (providerType) {
            case "openai", "deepseek", "moonshot", "zhipu", "qwen", "minimax",
                 "openrouter", "siliconflow", "stepfun", "doubao", "azure",
                 "baichuan", "google" -> true;
            default -> false;
        };
    }

    private ChatCompletionResponse executeCompletion(
            String providerType,
            ApiKeyPool.ProviderConfig config,
            ApiKeyPool.KeyEntry keyEntry,
            ChatCompletionRequest request) {

        List<Message> messages = convertMessages(request.getMessages());

        if ("anthropic".equals(providerType)) {
            return executeAnthropic(config, keyEntry, request, messages);
        }
        if (isOpenAiCompatible(providerType)) {
            return executeOpenAiCompatible(config, keyEntry, request, messages);
        }
        throw new IllegalArgumentException("Unsupported provider type: " + providerType);
    }

    private ChatCompletionResponse executeOpenAiCompatible(
            ApiKeyPool.ProviderConfig config,
            ApiKeyPool.KeyEntry keyEntry,
            ChatCompletionRequest request,
            List<Message> messages) {

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(keyEntry.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(request.getModel())
            .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
            .topP(request.getTopP() != null ? request.getTopP() : 1.0)
            .build();

        Prompt prompt = new Prompt(messages, options);
        ChatResponse response = chatModel.call(prompt);

        return buildResponse(request.getModel(), response);
    }

    private ChatCompletionResponse executeAnthropic(
            ApiKeyPool.ProviderConfig config,
            ApiKeyPool.KeyEntry keyEntry,
            ChatCompletionRequest request,
            List<Message> messages) {

        AnthropicApi anthropicApi = new AnthropicApi(keyEntry.getApiKey());

        AnthropicChatModel chatModel = AnthropicChatModel.builder()
            .anthropicApi(anthropicApi)
            .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .model(request.getModel())
            .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
            .topP(request.getTopP() != null ? request.getTopP() : 1.0)
            .build();

        Prompt prompt = new Prompt(messages, options);
        ChatResponse response = chatModel.call(prompt);

        return buildResponse(request.getModel(), response);
    }

    private void executeStreamCompletion(
            String providerType,
            ApiKeyPool.ProviderConfig config,
            ApiKeyPool.KeyEntry keyEntry,
            ChatCompletionRequest request,
            SseEmitter emitter) {

        List<Message> messages = convertMessages(request.getMessages());

        Flux<ChatResponse> stream;

        if (isOpenAiCompatible(providerType)) {
            OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(keyEntry.getApiKey())
                .baseUrl(config.getBaseUrl())
                .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(request.getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
                .build();

            Prompt prompt = new Prompt(messages, options);
            stream = chatModel.stream(prompt);
        } else if ("anthropic".equals(providerType)) {
            AnthropicApi anthropicApi = new AnthropicApi(keyEntry.getApiKey());

            AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .build();

            AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(request.getModel())
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
                .build();

            Prompt prompt = new Prompt(messages, options);
            stream = chatModel.stream(prompt);
        } else {
            throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }

        final Long keyId = keyEntry.getId();

        stream.subscribe(
            response -> {
                try {
                    String content = response.getResult().getOutput().getText();
                    if (content != null) {
                        emitter.send(SseEmitter.event()
                            .data(buildStreamChunk(request.getModel(), content)));
                    }
                } catch (IOException e) {
                    log.error("Error sending SSE event", e);
                }
            },
            error -> {
                log.error("Stream error", error);
                apiKeyPool.recordError(keyId, error.getMessage());
                emitter.completeWithError(error);
            },
            () -> {
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                    apiKeyPool.recordSuccess(keyId, 0); // Token count not available in stream
                } catch (IOException e) {
                    log.error("Error completing stream", e);
                }
            }
        );
    }

    private List<Message> convertMessages(List<ChatCompletionRequest.Message> requestMessages) {
        List<Message> messages = new ArrayList<>();
        for (ChatCompletionRequest.Message msg : requestMessages) {
            String content = msg.getContent() instanceof String
                ? (String) msg.getContent()
                : msg.getContent().toString();

            switch (msg.getRole().toLowerCase()) {
                case "system" -> messages.add(new SystemMessage(content));
                case "user" -> messages.add(new UserMessage(content));
                case "assistant" -> messages.add(new AssistantMessage(content));
            }
        }
        return messages;
    }

    private ChatCompletionResponse buildResponse(String model, ChatResponse response) {
        String content = response.getResult().getOutput().getText();

        ChatCompletionResponse.Message message = ChatCompletionResponse.Message.builder()
            .role("assistant")
            .content(content)
            .build();

        ChatCompletionResponse.Choice choice = ChatCompletionResponse.Choice.builder()
            .index(0)
            .message(message)
            .finishReason("stop")
            .build();

        int promptTokens = response.getMetadata().getUsage() != null
            ? response.getMetadata().getUsage().getPromptTokens().intValue() : 0;
        int completionTokens = response.getMetadata().getUsage() != null
            ? response.getMetadata().getUsage().getGenerationTokens().intValue() : 0;

        ChatCompletionResponse.Usage usage = ChatCompletionResponse.Usage.builder()
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .totalTokens(promptTokens + completionTokens)
            .build();

        return ChatCompletionResponse.builder()
            .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(List.of(choice))
            .usage(usage)
            .build();
    }

    private String buildStreamChunk(String model, String content) {
        return String.format(
            "{\"id\":\"chatcmpl-stream\",\"object\":\"chat.completion.chunk\"," +
            "\"model\":\"%s\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"%s\"}}]}",
            model, content.replace("\"", "\\\"").replace("\n", "\\n")
        );
    }

    /**
     * Get available providers and their status
     */
    public Map<String, Object> getProvidersStatus() {
        return apiKeyPool.getStatistics();
    }
}
