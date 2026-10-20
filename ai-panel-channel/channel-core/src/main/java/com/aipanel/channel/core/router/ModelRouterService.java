package com.aipanel.channel.core.router;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Model router service interface
 * Routes requests to appropriate AI providers using Spring AI
 */
public interface ModelRouterService {

    /**
     * Route chat completion request to appropriate provider
     */
    ChatCompletionResponse chatCompletion(ChatCompletionRequest request);

    /**
     * Stream chat completion response
     */
    SseEmitter streamChatCompletion(ChatCompletionRequest request);

    /**
     * Get list of available models
     */
    ModelListResponse listModels();

    /**
     * Get supported models for a specific provider
     */
    List<String> getProviderModels(String provider);

    /**
     * Check if model is supported
     */
    boolean isModelSupported(String model);
}
