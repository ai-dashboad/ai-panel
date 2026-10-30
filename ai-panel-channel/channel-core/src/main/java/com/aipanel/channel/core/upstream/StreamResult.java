package com.aipanel.channel.core.upstream;

import com.aipanel.channel.api.dto.response.ChatCompletionResponse;
import lombok.Data;

/**
 * Summary of a finished stream, used for usage accounting.
 */
@Data
public class StreamResult {
    private String model;
    private String finishReason;
    /** Usage reported by the upstream, or {@code null} when none was sent. */
    private ChatCompletionResponse.Usage usage;
    /** Generated text (content + tool call arguments), used to estimate usage when none was reported. */
    private final StringBuilder generatedText = new StringBuilder();
    private int chunks;
}
