package com.aipanel.dispatch.core.client.impl;

import com.aipanel.common.util.ApiKeyGenerator;
import com.aipanel.dispatch.api.dto.request.ChatRequest;
import com.aipanel.dispatch.api.dto.response.ChatResponse;
import com.aipanel.dispatch.core.client.AiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Anthropic Claude API Client Implementation
 */
@Component
@Slf4j
public class AnthropicClient implements AiClient {

    @Value("${ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${ai.anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${ai.anthropic.timeout:60000}")
    private int timeout;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    private static final String ANTHROPIC_VERSION = "2024-01-01";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "claude-3-opus", "claude-3-sonnet", "claude-3-haiku",
        "claude-3-5-sonnet", "claude-3-5-haiku",
        "claude-opus-4", "claude-sonnet-4", "claude-sonnet-4-5",
        "claude-2.1", "claude-2.0", "claude-instant-1.2"
    );

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            log.info("Anthropic client initialized, baseUrl={}", baseUrl);
        } else {
            log.warn("Anthropic API key not configured");
        }
    }

    @Override
    public String getProviderCode() {
        return "anthropic";
    }

    @Override
    public boolean supportsModel(String model) {
        return SUPPORTED_MODELS.stream().anyMatch(m -> model.toLowerCase().contains(m.toLowerCase()));
    }

    @Override
    public Mono<ChatResponse> chat(String model, ChatRequest request) {
        if (webClient == null) {
            return Mono.error(new IllegalStateException("Anthropic client not configured"));
        }

        Map<String, Object> body = buildRequestBody(model, request, false);

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(response -> convertResponse(response, model))
            .doOnSuccess(r -> healthy.set(true))
            .doOnError(e -> {
                healthy.set(false);
                log.error("Anthropic API error: {}", e.getMessage());
            });
    }

    @Override
    public Flux<ChatResponse> chatStream(String model, ChatRequest request) {
        if (webClient == null) {
            return Flux.error(new IllegalStateException("Anthropic client not configured"));
        }

        Map<String, Object> body = buildRequestBody(model, request, true);

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: "))
            .map(line -> {
                try {
                    String json = line.substring(6);
                    JsonNode event = objectMapper.readTree(json);
                    return convertStreamEvent(event, model);
                } catch (Exception e) {
                    log.error("Failed to parse stream event: {}", e.getMessage());
                    return null;
                }
            })
            .filter(r -> r != null)
            .doOnComplete(() -> healthy.set(true))
            .doOnError(e -> {
                healthy.set(false);
                log.error("Anthropic stream error: {}", e.getMessage());
            });
    }

    @Override
    public boolean isHealthy() {
        return healthy.get() && webClient != null;
    }

    private Map<String, Object> buildRequestBody(String model, ChatRequest request, boolean stream) {
        // Extract system message and convert other messages
        String systemPrompt = null;
        List<Map<String, String>> messages = new ArrayList<>();

        for (var msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                systemPrompt = msg.getContent();
            } else {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048);
        body.put("stream", stream);

        if (systemPrompt != null) {
            body.put("system", systemPrompt);
        }
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop_sequences", request.getStop());
        }

        return body;
    }

    private ChatResponse convertResponse(JsonNode response, String model) {
        String id = response.has("id") ? response.get("id").asText() : ApiKeyGenerator.generateResponseId();

        String content = "";
        if (response.has("content") && response.get("content").isArray()) {
            JsonNode contentArray = response.get("content");
            if (contentArray.size() > 0 && contentArray.get(0).has("text")) {
                content = contentArray.get(0).get("text").asText();
            }
        }

        String stopReason = response.has("stop_reason") ? response.get("stop_reason").asText() : "stop";
        String finishReason = "end_turn".equals(stopReason) ? "stop" : stopReason;

        ChatResponse.Usage usage = null;
        if (response.has("usage")) {
            JsonNode usageNode = response.get("usage");
            usage = ChatResponse.Usage.builder()
                .promptTokens(usageNode.has("input_tokens") ? usageNode.get("input_tokens").asInt() : 0)
                .completionTokens(usageNode.has("output_tokens") ? usageNode.get("output_tokens").asInt() : 0)
                .totalTokens(
                    (usageNode.has("input_tokens") ? usageNode.get("input_tokens").asInt() : 0) +
                    (usageNode.has("output_tokens") ? usageNode.get("output_tokens").asInt() : 0)
                )
                .build();
        }

        return ChatResponse.builder()
            .id(id)
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(List.of(
                ChatResponse.Choice.builder()
                    .index(0)
                    .message(ChatResponse.Message.builder()
                        .role("assistant")
                        .content(content)
                        .build())
                    .finishReason(finishReason)
                    .build()
            ))
            .usage(usage)
            .build();
    }

    private ChatResponse convertStreamEvent(JsonNode event, String model) {
        String type = event.has("type") ? event.get("type").asText() : "";

        if ("content_block_delta".equals(type) && event.has("delta")) {
            JsonNode delta = event.get("delta");
            String text = delta.has("text") ? delta.get("text").asText() : "";

            return ChatResponse.builder()
                .id(ApiKeyGenerator.generateResponseId())
                .object("chat.completion.chunk")
                .model(model)
                .choices(List.of(
                    ChatResponse.Choice.builder()
                        .index(0)
                        .delta(ChatResponse.Message.builder()
                            .content(text)
                            .build())
                        .build()
                ))
                .build();
        }

        if ("message_stop".equals(type)) {
            return ChatResponse.builder()
                .id(ApiKeyGenerator.generateResponseId())
                .object("chat.completion.chunk")
                .model(model)
                .choices(List.of(
                    ChatResponse.Choice.builder()
                        .index(0)
                        .delta(ChatResponse.Message.builder().build())
                        .finishReason("stop")
                        .build()
                ))
                .build();
        }

        return null;
    }
}
