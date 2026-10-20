package com.aipanel.billing.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * Record Usage Request
 */
@Data
@Builder
@Schema(description = "Record Usage Request")
public class RecordUsageRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "API Key ID")
    private Long apiKeyId;

    @NotBlank(message = "Request ID is required")
    @Schema(description = "Request ID")
    private String requestId;

    @NotBlank(message = "Model is required")
    @Schema(description = "Model ID")
    private String model;

    @NotBlank(message = "Provider is required")
    @Schema(description = "Provider code")
    private String provider;

    @NotNull(message = "Input tokens is required")
    @Schema(description = "Input tokens")
    private Integer inputTokens;

    @NotNull(message = "Output tokens is required")
    @Schema(description = "Output tokens")
    private Integer outputTokens;

    @Schema(description = "Request latency in milliseconds")
    private Long latencyMs;

    @Schema(description = "Request status")
    private String status;
}
