package com.aipanel.channel.web.controller;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import com.aipanel.channel.api.dto.response.ModelListResponse;
import com.aipanel.channel.core.service.ChannelApiException;
import com.aipanel.channel.core.service.ChatCompletionService;
import com.aipanel.channel.core.upstream.ClientDisconnectedException;
import com.aipanel.channel.core.upstream.UpstreamException;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.web.advice.OpenAiErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenAI-compatible API controller.
 * <p>
 * {@code POST /v1/chat/completions} returns JSON, or Server-Sent Events when the request has
 * {@code "stream": true} (regardless of the Accept header, like OpenAI). Authentication, model
 * routing and quota checks run before anything is written, so those failures get a proper
 * HTTP status and an OpenAI-style error body.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "OpenAI-Compatible API", description = "OpenAI-compatible chat completions and models API")
public class OpenAICompatibleController {

    private static final byte[] DONE = "data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8);

    private final ChatCompletionService chatCompletionService;
    private final ObjectMapper objectMapper;

    /**
     * Chat completions endpoint (OpenAI-compatible)
     * POST /v1/chat/completions
     */
    @PostMapping("/chat/completions")
    @Operation(summary = "Create chat completion",
        description = "Creates a chat completion; set stream=true for Server-Sent Events")
    public ChatCompletionResponse createChatCompletion(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChatCompletionRequest request,
            HttpServletResponse response) throws IOException {

        boolean stream = Boolean.TRUE.equals(request.getStream());
        log.info("Chat completion request - model: {}, messages: {}, tools: {}, stream: {}",
                request.getModel(), request.getMessages().size(),
                request.getTools() == null ? 0 : request.getTools().size(), stream);

        ChannelApiKeyPO key = chatCompletionService.authorize(authorization, request);
        if (!stream) {
            return chatCompletionService.chatCompletion(key, request);
        }

        streamTo(response, key, request);
        // Response already written; returning null with an HttpServletResponse argument tells
        // Spring MVC the request was handled.
        return null;
    }

    private void streamTo(HttpServletResponse response, ChannelApiKeyPO key, ChatCompletionRequest request)
            throws IOException {
        response.setStatus(200);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        OutputStream out = response.getOutputStream();
        AtomicBoolean sent = new AtomicBoolean(false);

        try {
            chatCompletionService.streamChatCompletion(key, request, chunk -> {
                out.write(("data: " + chunk + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                sent.set(true);
            });
        } catch (ClientDisconnectedException e) {
            log.info("Client disconnected during stream (model: {})", request.getModel());
            return;
        } catch (RuntimeException e) {
            if (!sent.get() && !response.isCommitted()) {
                // Nothing written yet: let the error handler produce a normal JSON error response.
                response.reset();
                throw e;
            }
            log.warn("Stream failed after first chunk (model: {}): {}", request.getModel(), e.getMessage());
            writeStreamError(out, e);
        }
        try {
            out.write(DONE);
            out.flush();
        } catch (IOException e) {
            log.debug("Client went away before [DONE]");
        }
    }

    private void writeStreamError(OutputStream out, RuntimeException e) {
        String type = "api_error";
        String code = "upstream_error";
        if (e instanceof UpstreamException ue) {
            code = ue.getCode();
        } else if (e instanceof ChannelApiException ce) {
            type = ce.getType();
            code = ce.getCode();
        }
        try {
            String json = objectMapper.writeValueAsString(OpenAiErrorHandler.body(type, code,
                e.getMessage() == null ? "Upstream error" : e.getMessage()));
            out.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ignored) {
            // client gone
        }
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
     * Get model endpoint (OpenAI-compatible). Model ids may contain slashes (e.g. Qwen/Qwen3-32B).
     * GET /v1/models/{model}
     */
    @GetMapping("/models/{*model}")
    @Operation(summary = "Get model", description = "Retrieves a model instance")
    public ModelListResponse.ModelData getModel(
            @Parameter(description = "Authorization header with API key", required = true)
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Model ID", required = true)
            @PathVariable("model") String model) {

        String id = model.startsWith("/") ? model.substring(1) : model;
        log.info("Get model request - model: {}", id);
        ModelListResponse models = chatCompletionService.listModels(authorization);
        return models.getData().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ChannelApiException(404, "invalid_request_error", "model_not_found",
                    "The model '" + id + "' does not exist"));
    }
}
