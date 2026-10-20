package com.aipanel.dispatch.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat completion response DTO (OpenAI compatible)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat Completion Response")
public class ChatResponse {

    @Schema(description = "Response ID", example = "chatcmpl-abc123")
    private String id;

    @Schema(description = "Object type", example = "chat.completion")
    private String object;

    @Schema(description = "Creation timestamp", example = "1704067200")
    private Long created;

    @Schema(description = "Model used", example = "gpt-4")
    private String model;

    @Schema(description = "System fingerprint")
    private String systemFingerprint;

    @Schema(description = "Response choices")
    private List<Choice> choices;

    @Schema(description = "Token usage statistics")
    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response Choice")
    public static class Choice {

        @Schema(description = "Choice index", example = "0")
        private Integer index;

        @Schema(description = "Response message")
        private Message message;

        @Schema(description = "Streaming delta (for stream responses)")
        private Message delta;

        @Schema(description = "Log probabilities")
        private Object logprobs;

        @Schema(description = "Finish reason", example = "stop",
                allowableValues = {"stop", "length", "content_filter", "tool_calls"})
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response Message")
    public static class Message {

        @Schema(description = "Message role", example = "assistant")
        private String role;

        @Schema(description = "Message content")
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Token Usage")
    public static class Usage {

        @Schema(description = "Prompt tokens", example = "56")
        private Integer promptTokens;

        @Schema(description = "Completion tokens", example = "128")
        private Integer completionTokens;

        @Schema(description = "Total tokens", example = "184")
        private Integer totalTokens;
    }

    /**
     * Create a simple response
     */
    public static ChatResponse of(String id, String model, String content) {
        return ChatResponse.builder()
            .id(id)
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(model)
            .choices(List.of(
                Choice.builder()
                    .index(0)
                    .message(Message.builder()
                        .role("assistant")
                        .content(content)
                        .build())
                    .finishReason("stop")
                    .build()
            ))
            .build();
    }
}
