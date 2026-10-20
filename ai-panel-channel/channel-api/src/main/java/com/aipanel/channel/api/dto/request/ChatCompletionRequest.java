package com.aipanel.channel.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion request
 */
@Data
@Schema(description = "Chat completion request (OpenAI-compatible)")
public class ChatCompletionRequest {

    @NotBlank(message = "Model is required")
    @Schema(description = "Model ID to use", example = "gpt-4")
    private String model;

    @NotEmpty(message = "Messages cannot be empty")
    @Schema(description = "List of messages in the conversation")
    private List<Message> messages;

    @Schema(description = "Sampling temperature (0-2)", example = "0.7")
    private Double temperature;

    @JsonProperty("top_p")
    @Schema(description = "Nucleus sampling probability", example = "1.0")
    private Double topP;

    @Schema(description = "Number of completions to generate", example = "1")
    private Integer n;

    @Schema(description = "Whether to stream partial message deltas", example = "false")
    private Boolean stream;

    @Schema(description = "Stop sequences")
    private List<String> stop;

    @JsonProperty("max_tokens")
    @Schema(description = "Maximum tokens to generate", example = "4096")
    private Integer maxTokens;

    @JsonProperty("presence_penalty")
    @Schema(description = "Presence penalty (-2.0 to 2.0)", example = "0")
    private Double presencePenalty;

    @JsonProperty("frequency_penalty")
    @Schema(description = "Frequency penalty (-2.0 to 2.0)", example = "0")
    private Double frequencyPenalty;

    @JsonProperty("logit_bias")
    @Schema(description = "Token logit bias")
    private Map<String, Integer> logitBias;

    @Schema(description = "User identifier for abuse detection")
    private String user;

    @Schema(description = "Tool definitions for function calling")
    private List<Tool> tools;

    @JsonProperty("tool_choice")
    @Schema(description = "Tool choice mode")
    private Object toolChoice;

    @JsonProperty("response_format")
    @Schema(description = "Response format specification")
    private ResponseFormat responseFormat;

    @Data
    @Schema(description = "Chat message")
    public static class Message {
        @NotBlank(message = "Role is required")
        @Schema(description = "Message role", example = "user")
        private String role;

        @Schema(description = "Message content")
        private Object content;

        @Schema(description = "Tool call ID (for tool responses)")
        @JsonProperty("tool_call_id")
        private String toolCallId;

        @Schema(description = "Tool calls made by assistant")
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        @Schema(description = "Name of the participant")
        private String name;
    }

    @Data
    @Schema(description = "Tool definition")
    public static class Tool {
        @Schema(description = "Tool type", example = "function")
        private String type;

        @Schema(description = "Function definition")
        private Function function;
    }

    @Data
    @Schema(description = "Function definition")
    public static class Function {
        @Schema(description = "Function name")
        private String name;

        @Schema(description = "Function description")
        private String description;

        @Schema(description = "Function parameters JSON schema")
        private Map<String, Object> parameters;
    }

    @Data
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
    @Schema(description = "Function call details")
    public static class FunctionCall {
        @Schema(description = "Function name")
        private String name;

        @Schema(description = "Function arguments as JSON string")
        private String arguments;
    }

    @Data
    @Schema(description = "Response format")
    public static class ResponseFormat {
        @Schema(description = "Format type", example = "json_object")
        private String type;
    }
}
