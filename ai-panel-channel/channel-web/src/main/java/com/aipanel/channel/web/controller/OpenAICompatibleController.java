package com.aipanel.channel.web.controller;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.service.ChatCompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * OpenAI-compatible API controller
 * This controller provides endpoints compatible with OpenAI API format,
 * allowing tools like Claude Code, VS Code extensions, and Cursor to work seamlessly.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "OpenAI-Compatible API", description = "OpenAI-compatible chat completions and models API")
public class OpenAICompatibleController {

    private final ChatCompletionService chatCompletionService;

    /**
     * Chat completions endpoint (OpenAI-compatible)
     * POST /v1/chat/completions
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create chat completion", description = "Creates a chat completion using the specified model")
    public Object createChatCompletion(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChatCompletionRequest request) {

        log.info("Chat completion request - model: {}, messages: {}, stream: {}",
                request.getModel(), request.getMessages().size(),
                Boolean.TRUE.equals(request.getStream()));

        if (Boolean.TRUE.equals(request.getStream())) {
            return streamChatCompletion(authorization, request);
        }

        return chatCompletionService.chatCompletion(authorization, request);
    }

    /**
     * Stream chat completions endpoint
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat completion", description = "Creates a streaming chat completion")
    public SseEmitter streamChatCompletion(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChatCompletionRequest request) {

        log.info("Streaming chat completion request - model: {}", request.getModel());
        return chatCompletionService.streamChatCompletion(authorization, request);
    }

    /**
     * List models endpoint (OpenAI-compatible)
     * GET /v1/models
     */
    @GetMapping("/models")
    @Operation(summary = "List models", description = "Lists available models")
    public ModelListResponse listModels(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization) {

        log.info("List models request");
        return chatCompletionService.listModels(authorization);
    }

    /**
     * Get model endpoint (OpenAI-compatible)
     * GET /v1/models/{model}
     */
    @GetMapping("/models/{model}")
    @Operation(summary = "Get model", description = "Retrieves a model instance")
    public ModelListResponse.ModelData getModel(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Model ID", required = true)
            @PathVariable String model) {

        log.info("Get model request - model: {}", model);
        ModelListResponse models = chatCompletionService.listModels(authorization);
        return models.getData().stream()
                .filter(m -> m.getId().equals(model))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Model not found: " + model));
    }
}
