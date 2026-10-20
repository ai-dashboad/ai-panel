package com.aipanel.dispatch.core.client.impl;

import com.aipanel.common.util.ApiKeyGenerator;
import com.aipanel.dispatch.api.dto.request.ChatMessage;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenAI API Client Implementation
 */
@Component
@Slf4j
public class OpenAiClient implements AiClient {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.openai.timeout:60000}")
    private int timeout;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "gpt-4", "gpt-4-turbo", "gpt-4-turbo-preview", "gpt-4o", "gpt-4o-mini", "gpt-4.1",
        "gpt-3.5-turbo", "gpt-3.5-turbo-16k",
        "o1", "o1-mini", "o1-preview", "o3-mini"
    );

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
            log.info("OpenAI client initialized, baseUrl={}", baseUrl);
        } else {
            log.warn("OpenAI API key not configured");
        }
    }

    @Override
    public String getProviderCode() {
        return "openai";
    }

    @Override
    public boolean supportsModel(String model) {
        return SUPPORTED_MODELS.stream().anyMatch(m -> model.toLowerCase().startsWith(m));
    }

    @Override
    public Mono<ChatResponse> chat(String model, ChatRequest request) {
        if (webClient == null) {
            return Mono.error(new IllegalStateException("OpenAI client not configured"));
        }

        Map<String, Object> body = buildRequestBody(model, request, false);

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(timeout))
            .map(response -> convertResponse(response, model))
            .doOnSuccess(r -> healthy.set(true))
            .doOnError(e -> {
                healthy.set(false);
                log.error("OpenAI API error: {}", e.getMessage());
            });
    }

    @Override
    public Flux<ChatResponse> chatStream(String model, ChatRequest request) {
        if (webClient == null) {
            return Flux.error(new IllegalStateException("OpenAI client not configured"));
        }

        Map<String, Object> body = buildRequestBody(model, request, true);

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
            .map(line -> {
                try {
                    String json = line.substring(6);
                    JsonNode chunk = objectMapper.readTree(json);
                    return convertStreamChunk(chunk, model);
                } catch (Exception e) {
                    log.error("Failed to parse stream chunk: {}", e.getMessage());
                    return null;
                }
            })
            .filter(r -> r != null)
            .doOnComplete(() -> healthy.set(true))
            .doOnError(e -> {
                healthy.set(false);
                log.error("OpenAI stream error: {}", e.getMessage());
            });
    }

    @Override
    public boolean isHealthy() {
        return healthy.get() && webClient != null;
    }

    private Map<String, Object> buildRequestBody(String model, ChatRequest request, boolean stream) {
        List<Map<String, String>> messages = request.getMessages().stream()
            .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
            .toList();

        return Map.of(
            "model", model,
            "messages", messages,
            "temperature", request.getTemperature() != null ? request.getTemperature() : 0.7,
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048,
            "stream", stream,
            "n", request.getN() != null ? request.getN() : 1
        );
    }

    private ChatResponse convertResponse(JsonNode response, String model) {
        String id = response.has("id") ? response.get("id").asText() : ApiKeyGenerator.generateResponseId();
        long created = response.has("created") ? response.get("created").asLong() : System.currentTimeMillis() / 1000;

        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
            .id(id)
            .object("chat.completion")
            .created(created)
            .model(model);

        if (response.has("choices") && response.get("choices").isArray()) {
            JsonNode choicesNode = response.get("choices");
            List<ChatResponse.Choice> choices = new java.util.ArrayList<>();

            for (JsonNode choiceNode : choicesNode) {
                ChatResponse.Choice choice = ChatResponse.Choice.builder()
                    .index(choiceNode.has("index") ? choiceNode.get("index").asInt() : 0)
                    .finishReason(choiceNode.has("finish_reason") ? choiceNode.get("finish_reason").asText() : null)
                    .build();

                if (choiceNode.has("message")) {
                    JsonNode messageNode = choiceNode.get("message");
                    choice.setMessage(ChatResponse.Message.builder()
                        .role(messageNode.has("role") ? messageNode.get("role").asText() : "assistant")
                        .content(messageNode.has("content") ? messageNode.get("content").asText() : "")
                        .build());
                }
                choices.add(choice);
            }
            builder.choices(choices);
        }

        if (response.has("usage")) {
            JsonNode usageNode = response.get("usage");
            builder.usage(ChatResponse.Usage.builder()
                .promptTokens(usageNode.has("prompt_tokens") ? usageNode.get("prompt_tokens").asInt() : 0)
                .completionTokens(usageNode.has("completion_tokens") ? usageNode.get("completion_tokens").asInt() : 0)
                .totalTokens(usageNode.has("total_tokens") ? usageNode.get("total_tokens").asInt() : 0)
                .build());
        }

        return builder.build();
    }

    private ChatResponse convertStreamChunk(JsonNode chunk, String model) {
        String id = chunk.has("id") ? chunk.get("id").asText() : "";

        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
            .id(id)
            .object("chat.completion.chunk")
            .model(model);

        if (chunk.has("choices") && chunk.get("choices").isArray()) {
            JsonNode choiceNode = chunk.get("choices").get(0);

            ChatResponse.Choice choice = ChatResponse.Choice.builder()
                .index(0)
                .finishReason(choiceNode.has("finish_reason") && !choiceNode.get("finish_reason").isNull() ?
                    choiceNode.get("finish_reason").asText() : null)
                .build();

            if (choiceNode.has("delta")) {
                JsonNode deltaNode = choiceNode.get("delta");
                choice.setDelta(ChatResponse.Message.builder()
                    .role(deltaNode.has("role") ? deltaNode.get("role").asText() : null)
                    .content(deltaNode.has("content") ? deltaNode.get("content").asText() : null)
                    .build());
            }

            builder.choices(List.of(choice));
        }

        return builder.build();
    }
}
