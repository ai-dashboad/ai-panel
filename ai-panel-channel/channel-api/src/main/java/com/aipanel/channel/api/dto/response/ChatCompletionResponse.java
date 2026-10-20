package com.aipanel.channel.api.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion response.
 * Unknown fields returned by an upstream are preserved via {@code extra}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Builder.Default
    @Schema(hidden = true)
    private Map<String, Object> extra = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtra(String key, Object value) {
        if (extra == null) {
            extra = new LinkedHashMap<>();
        }
        extra.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Completion choice")
    public static class Choice {
        @Schema(description = "Choice index")
        private Integer index;

        @Schema(description = "Generated message")
        private Message message;

        @Schema(description = "Delta (streaming chunks only)")
        private Message delta;

        @Schema(description = "Finish reason: stop, length, tool_calls, content_filter")
        @JsonProperty("finish_reason")
        private String finishReason;

        @Schema(description = "Log probabilities")
        private Object logprobs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Response message")
    public static class Message {
        @Schema(description = "Message role", example = "assistant")
        private String role;

        @Schema(description = "Message content")
        private String content;

        @Schema(description = "Reasoning content (DeepSeek-R1 / Qwen3 style reasoning models)")
        @JsonProperty("reasoning_content")
        private String reasoningContent;

        @Schema(description = "Refusal message")
        private String refusal;

        @Schema(description = "Tool calls")
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        @Builder.Default
        @Schema(hidden = true)
        private Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnySetter
        public void putExtra(String key, Object value) {
            if (extra == null) {
                extra = new LinkedHashMap<>();
            }
            extra.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, Object> getExtra() {
            return extra;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Tool call")
    public static class ToolCall {
        @Schema(description = "Index (streaming deltas only)")
        private Integer index;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
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
