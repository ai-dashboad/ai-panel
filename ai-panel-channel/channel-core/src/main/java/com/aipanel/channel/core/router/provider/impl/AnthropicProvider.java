package com.aipanel.channel.core.router.provider.impl;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.core.router.provider.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic (Claude) provider implementation using Spring AI
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.ai.anthropic", name = "api-key")
public class AnthropicProvider implements AiProvider {

    private static final List<String> SUPPORTED_MODELS = List.of(
        "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307",
        "claude-3-5-sonnet-20240620", "claude-3-opus", "claude-3-sonnet", "claude-3-haiku"
    );

    @Autowired(required = false)
    private AnthropicChatModel chatModel;

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public List<String> getSupportedModels() {
        return SUPPORTED_MODELS;
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletionRequest request) {
        if (chatModel == null) {
            throw new IllegalStateException("Anthropic chat model not configured");
        }

        List<Message> messages = convertMessages(request.getMessages());

        // Map model aliases to actual model IDs
        String modelId = mapModelId(request.getModel());

        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .model(modelId)
            .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
            .topP(request.getTopP() != null ? request.getTopP() : 1.0)
            .build();

        Prompt prompt = new Prompt(messages, options);
        ChatResponse response = chatModel.call(prompt);

        return buildResponse(request.getModel(), response);
    }

    @Override
    public void streamChatCompletion(ChatCompletionRequest request, SseEmitter emitter) {
        if (chatModel == null) {
            throw new IllegalStateException("Anthropic chat model not configured");
        }

        List<Message> messages = convertMessages(request.getMessages());
        String modelId = mapModelId(request.getModel());

        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .model(modelId)
            .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096)
            .build();

        Prompt prompt = new Prompt(messages, options);

        Flux<ChatResponse> stream = chatModel.stream(prompt);

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
                emitter.completeWithError(error);
            },
            () -> {
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Error completing stream", e);
                }
            }
        );
    }

    @Override
    public boolean isAvailable() {
        return chatModel != null;
    }

    private String mapModelId(String model) {
        return switch (model) {
            case "claude-3-opus" -> "claude-3-opus-20240229";
            case "claude-3-sonnet" -> "claude-3-sonnet-20240229";
            case "claude-3-haiku" -> "claude-3-haiku-20240307";
            default -> model;
        };
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
}
