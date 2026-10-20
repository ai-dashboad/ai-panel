package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Product SKU Request")
public class ProductSkuRequest {

    @Size(max = 30, message = "Platform must not exceed 30 characters")
    @Schema(description = "Platform identifier (null for all platforms)", example = "taobao")
    private String platform;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @Schema(description = "Product SKU code", example = "API-PRO-30D", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Product name", example = "API Pro 30天套餐", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Product description", example = "包含100万tokens的API调用额度")
    private String description;

    @Schema(description = "Quota type: tokens or requests", example = "tokens")
    private String quotaType = "tokens";

    @Min(value = 0, message = "Quota tokens must be non-negative")
    @Schema(description = "Token quota amount", example = "1000000")
    private Long quotaTokens;

    @Min(value = 0, message = "Quota requests must be non-negative")
    @Schema(description = "Request quota amount", example = "10000")
    private Long quotaRequests;

    @Min(value = 1, message = "Validity days must be at least 1")
    @Schema(description = "API key validity in days", example = "30")
    private Integer validityDays = 30;

    @Schema(description = "Allowed models (comma-separated, null for all)", example = "gpt-4o,claude-3-5-sonnet")
    private String allowedModels;

    @Min(value = 0, message = "Rate limit must be non-negative")
    @Schema(description = "Rate limit per minute", example = "60")
    private Integer rateLimit;

    @Schema(description = "Enable this SKU", example = "true")
    private Boolean enabled = true;
}
