package com.aipanel.dispatch.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Model information response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Model Information")
public class ModelInfoResponse {

    @Schema(description = "Model ID", example = "gpt-4")
    private String id;

    @Schema(description = "Display name", example = "GPT-4")
    private String displayName;

    @Schema(description = "Model provider", example = "openai")
    private String provider;

    @Schema(description = "Model type", example = "chat")
    private String modelType;

    @Schema(description = "Maximum context window", example = "128000")
    private Integer contextWindow;

    @Schema(description = "Maximum output tokens", example = "4096")
    private Integer maxOutputTokens;

    @Schema(description = "Input price per 1K tokens (USD)", example = "0.03")
    private BigDecimal inputPrice;

    @Schema(description = "Output price per 1K tokens (USD)", example = "0.06")
    private BigDecimal outputPrice;

    @Schema(description = "Supported features")
    private List<String> features;

    @Schema(description = "Whether the model is deprecated", example = "false")
    private Boolean deprecated;

    @Schema(description = "Model description")
    private String description;

    @Schema(description = "Pricing group code")
    private String groupCode;

    @Schema(description = "Pricing group name")
    private String groupName;

    @Schema(description = "Group price multiplier")
    private BigDecimal priceMultiplier;

    @Schema(description = "Employee sell input price per 1M tokens (USD)")
    private BigDecimal sellInputPricePerMillion;

    @Schema(description = "Employee sell output price per 1M tokens (USD)")
    private BigDecimal sellOutputPricePerMillion;

    @Schema(description = "Endpoint / filter labels")
    private List<String> labels;

    @Schema(description = "Pricing type: usage | per_request | task")
    private String pricingType;
}
