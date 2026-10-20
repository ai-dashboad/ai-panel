package com.aipanel.model.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Update Model Request
 */
@Data
@Schema(description = "Update Model Request")
public class UpdateModelRequest {

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Context window size")
    private Integer contextWindow;

    @Schema(description = "Max output tokens")
    private Integer maxOutputTokens;

    @Schema(description = "Input price per 1K tokens")
    private BigDecimal inputPrice;

    @Schema(description = "Output price per 1K tokens")
    private BigDecimal outputPrice;

    @Schema(description = "Supported features")
    private List<String> features;

    @Schema(description = "Fallback chain model IDs")
    private List<String> fallbackChain;

    @Schema(description = "Model description")
    private String description;

    @Schema(description = "Whether model is enabled")
    private Boolean enabled;

    @Schema(description = "Whether model is deprecated")
    private Boolean deprecated;
}
