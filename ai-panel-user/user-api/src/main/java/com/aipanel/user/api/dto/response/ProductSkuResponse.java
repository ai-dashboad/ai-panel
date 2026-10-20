package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product SKU Response")
public class ProductSkuResponse {

    @Schema(description = "SKU ID", example = "1")
    private Long id;

    @Schema(description = "Platform identifier (null for all platforms)", example = "taobao")
    private String platform;

    @Schema(description = "Product SKU code", example = "API-PRO-30D")
    private String sku;

    @Schema(description = "Product name", example = "API Pro 30天套餐")
    private String name;

    @Schema(description = "Product description")
    private String description;

    @Schema(description = "Quota type: tokens or requests", example = "tokens")
    private String quotaType;

    @Schema(description = "Token quota amount", example = "1000000")
    private Long quotaTokens;

    @Schema(description = "Request quota amount", example = "10000")
    private Long quotaRequests;

    @Schema(description = "API key validity in days", example = "30")
    private Integer validityDays;

    @Schema(description = "Allowed models (comma-separated)")
    private String allowedModels;

    @Schema(description = "Rate limit per minute", example = "60")
    private Integer rateLimit;

    @Schema(description = "SKU enabled status")
    private Boolean enabled;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
