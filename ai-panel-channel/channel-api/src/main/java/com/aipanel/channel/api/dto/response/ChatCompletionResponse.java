package com.aipanel.channel.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI-compatible chat completion response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat completion response (OpenAI-compatible)")
public class ChatCompletionResponse {

    @Schema(description = "Unique identifier for this completion")
    private String id;

    @Schema(description = "Object type", example = "chat.completion")
    private String object;

    @Schema(description = "Unix timestamp of creation")
    private Long created;

    @Schema(description = "Model used for completion")
    private String model;

    @Schema(description = "List of completion choices")
    private List<Choice> choices;

    @Schema(description = "Token usage statistics")
    private Usage usage;

    @JsonProperty("system_fingerprint")
    @Schema(description = "System fingerprint")
    private String systemFingerprint;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Completion choice")
    public static class Choice {
        @Schema(description = "Choice index")
        private Integer index;

        @Schema(description = "Generated message")
        private Message message;

        @Schema(description = "Finish reason")
        @JsonProperty("finish_reason")
        private String finishReason;

        @Schema(description = "Log probabilities")
        private Object logprobs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response message")
    public static class Message {
        @Schema(description = "Message role", example = "assistant")
        private String role;

        @Schema(description = "Message content")
        private String content;

        @Schema(description = "Tool calls")
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Tool call")
    public static class ToolCall {
        @Schema(description = "Tool call ID")
        private String id;

        @Schema(description = "Tool type")
        private String type;

        @Schema(description = "Function call details")
        private FunctionCall function;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Function call details")
    public static class FunctionCall {
        @Schema(description = "Function name")
        private String name;

        @Schema(description = "Function arguments as JSON string")
        private String arguments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Token usage statistics")
    public static class Usage {
        @JsonProperty("prompt_tokens")
        @Schema(description = "Number of tokens in prompt")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        @Schema(description = "Number of tokens in completion")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        @Schema(description = "Total tokens used")
        private Integer totalTokens;
    }
}
