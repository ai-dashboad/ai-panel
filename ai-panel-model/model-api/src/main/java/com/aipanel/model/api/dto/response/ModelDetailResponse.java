package com.aipanel.model.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Model Detail Response
 */
@Data
@Builder
@Schema(description = "Model Detail Response")
public class ModelDetailResponse {

    @Schema(description = "Model ID")
    private String id;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Provider code")
    private String providerCode;

    @Schema(description = "Provider name")
    private String providerName;

    @Schema(description = "Model type")
    private String modelType;

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

    @Schema(description = "Fallback chain")
    private List<String> fallbackChain;

    @Schema(description = "Whether model is enabled")
    private Boolean enabled;

    @Schema(description = "Whether model is deprecated")
    private Boolean deprecated;

    @Schema(description = "Model description")
    private String description;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
