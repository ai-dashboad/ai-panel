package com.aipanel.model.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create Model Request
 */
@Data
@Schema(description = "Create Model Request")
public class CreateModelRequest {

    @NotBlank(message = "Model ID is required")
    @Schema(description = "Model ID", example = "gpt-4o")
    private String modelId;

    @NotBlank(message = "Display name is required")
    @Schema(description = "Display name", example = "GPT-4o")
    private String displayName;

    @NotBlank(message = "Provider code is required")
    @Schema(description = "Provider code", example = "openai")
    private String providerCode;

    @NotBlank(message = "Model type is required")
    @Schema(description = "Model type", example = "CHAT")
    private String modelType;

    @Schema(description = "Context window size", example = "128000")
    private Integer contextWindow;

    @Schema(description = "Max output tokens", example = "4096")
    private Integer maxOutputTokens;

    @NotNull(message = "Input price is required")
    @Schema(description = "Input price per 1K tokens", example = "0.005")
    private BigDecimal inputPrice;

    @NotNull(message = "Output price is required")
    @Schema(description = "Output price per 1K tokens", example = "0.015")
    private BigDecimal outputPrice;

    @Schema(description = "Supported features")
    private List<String> features;

    @Schema(description = "Fallback chain model IDs")
    private List<String> fallbackChain;

    @Schema(description = "Model description")
    private String description;
}
