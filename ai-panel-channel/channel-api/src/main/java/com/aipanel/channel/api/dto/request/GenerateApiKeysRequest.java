package com.aipanel.channel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch generate API keys request
 */
@Data
@Schema(description = "Generate API keys request")
public class GenerateApiKeysRequest {

    @NotNull(message = "Channel ID is required")
    @Schema(description = "Channel ID to associate keys with")
    private Long channelId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Minimum quantity is 1")
    @Max(value = 1000, message = "Maximum quantity is 1000")
    @Schema(description = "Number of keys to generate", example = "10")
    private Integer quantity;

    @Schema(description = "Plan type (free, basic, pro, enterprise)", example = "basic")
    private String planType;

    @Schema(description = "Monthly quota in tokens", example = "1000000")
    private Long monthlyQuota;

    @Schema(description = "Rate limit (requests per minute)", example = "60")
    private Integer rateLimit;

    @Schema(description = "Allowed models (null for all)")
    private List<String> allowedModels;

    @Schema(description = "Expiration date (null for never)")
    private LocalDateTime expiresAt;

    @Schema(description = "Price per key", example = "9.99")
    private BigDecimal price;

    @Schema(description = "Key name prefix", example = "batch-001")
    private String namePrefix;
}
