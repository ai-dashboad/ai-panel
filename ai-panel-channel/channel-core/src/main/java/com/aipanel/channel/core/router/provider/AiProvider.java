package com.aipanel.channel.core.router.provider;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI provider interface
 */
public interface AiProvider {

    /**
     * Get provider name
     */
    String getName();

    /**
     * Get supported models
     */
    List<String> getSupportedModels();

    /**
     * Execute chat completion request
     */
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request);

    /**
     * Stream chat completion response
     */
    void streamChatCompletion(ChatCompletionRequest request, SseEmitter emitter);

    /**
     * Check if provider is available
     */
    boolean isAvailable();
}
