package com.aipanel.channel.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion request.
 * <p>
 * Only fields the client actually sent are serialized when the request is
 * forwarded upstream (no forced defaults). Unknown top-level fields
 * (e.g. {@code top_k}, {@code chat_template_kwargs}, {@code logprobs}) are kept
 * in {@link #extra} and passed through to OpenAI-compatible upstreams.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    @Schema(description = "Stop sequence(s): a string or an array of strings")
    private Object stop;

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

    @Schema(description = "Tool definitions for function calling (OpenAI format, passed through as-is)")
    private List<Map<String, Object>> tools;

    @JsonProperty("tool_choice")
    @Schema(description = "Tool choice: \"auto\", \"none\", \"required\" or {type:function,function:{name}}")
    private Object toolChoice;

    @JsonProperty("parallel_tool_calls")
    @Schema(description = "Whether the model may call several tools in one turn")
    private Boolean parallelToolCalls;

    @JsonProperty("response_format")
    @Schema(description = "Response format specification (passed through as-is)")
    private Object responseFormat;

    @JsonProperty("stream_options")
    @Schema(description = "Streaming options, e.g. {\"include_usage\": true}")
    private Map<String, Object> streamOptions;

    @Schema(description = "Sampling seed")
    private Long seed;

    /**
     * Any other top-level fields sent by the client. Passed through to
     * OpenAI-compatible upstreams.
     */
    @JsonIgnore
    @Schema(hidden = true)
    private Map<String, Object> extra = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtra(String key, Object value) {
        extra.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

        @JsonIgnore
        @Schema(hidden = true)
        private Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnySetter
        public void putExtra(String key, Object value) {
            extra.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getExtra() {
            return extra;
        }
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
