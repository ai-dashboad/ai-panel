package com.aipanel.channel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Register / update a model served by a provider. {@code modelId} is the exact name the
 * upstream expects and clients send, e.g. {@code Qwen/Qwen3-32B}, {@code glm-4.5}, {@code deepseek-r1:32b}.
 */
@Data
public class ProviderModelRequest {
    @NotBlank(message = "modelId is required")
    @Size(max = 100, message = "modelId must be at most 100 characters")
    private String modelId;
    @Size(max = 100)
    private String modelName;
    private Boolean enabled = true;
    private BigDecimal inputPricePerMillion;
    private BigDecimal outputPricePerMillion;
    private Integer maxTokens;
    private Integer contextWindow;
    private Boolean supportsVision;
    private Boolean supportsFunctionCall;
}
