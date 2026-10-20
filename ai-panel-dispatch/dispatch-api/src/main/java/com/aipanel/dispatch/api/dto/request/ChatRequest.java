package com.aipanel.dispatch.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Chat completion request DTO (OpenAI compatible)
 */
@Data
@Schema(description = "Chat Completion Request")
public class ChatRequest {

    @NotBlank(message = "Model is required")
    @Size(max = 64, message = "Model name max 64 characters")
    @Schema(description = "AI model name", example = "gpt-4", requiredMode = Schema.RequiredMode.REQUIRED)
    private String model;

    @NotEmpty(message = "Messages are required")
    @Size(min = 1, max = 100, message = "Messages count must be 1-100")
    @Valid
    @Schema(description = "Conversation messages", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ChatMessage> messages;

    @DecimalMin(value = "0.0", message = "Temperature min 0")
    @DecimalMax(value = "2.0", message = "Temperature max 2")
    @Schema(description = "Sampling temperature", example = "0.7", minimum = "0", maximum = "2")
    private Double temperature = 0.7;

    @Min(value = 1, message = "Max tokens min 1")
    @Max(value = 128000, message = "Max tokens max 128000")
    @Schema(description = "Maximum output tokens", example = "2048")
    private Integer maxTokens = 2048;

    @Schema(description = "Enable streaming output", example = "false")
    private Boolean stream = false;

    @DecimalMin(value = "0.0", message = "Top P min 0")
    @DecimalMax(value = "1.0", message = "Top P max 1")
    @Schema(description = "Nucleus sampling parameter", example = "1.0")
    private Double topP = 1.0;

    @Min(value = 1, message = "N min 1")
    @Max(value = 10, message = "N max 10")
    @Schema(description = "Number of completions to generate", example = "1")
    private Integer n = 1;

    @Size(max = 4, message = "Stop sequences max 4")
    @Schema(description = "Stop sequences")
    private List<@Size(max = 100) String> stop;

    @DecimalMin(value = "-2.0", message = "Presence penalty min -2")
    @DecimalMax(value = "2.0", message = "Presence penalty max 2")
    @Schema(description = "Presence penalty", example = "0")
    private Double presencePenalty = 0.0;

    @DecimalMin(value = "-2.0", message = "Frequency penalty min -2")
    @DecimalMax(value = "2.0", message = "Frequency penalty max 2")
    @Schema(description = "Frequency penalty", example = "0")
    private Double frequencyPenalty = 0.0;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "Invalid user format")
    @Schema(description = "User identifier for monitoring", example = "user-123")
    private String user;

    @Schema(description = "Allow model fallback on failure", example = "true")
    private Boolean allowFallback = true;
}
