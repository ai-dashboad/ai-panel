package com.aipanel.channel.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Create sales channel request
 */
@Data
@Schema(description = "Create channel request")
public class CreateChannelRequest {

    @NotBlank(message = "Channel name is required")
    @Schema(description = "Channel name", example = "Taobao Store")
    private String name;

    @NotBlank(message = "Channel type is required")
    @Schema(description = "Channel type (taobao, amazon, shopify, custom)", example = "taobao")
    private String type;

    @Schema(description = "Channel description")
    private String description;

    @Schema(description = "API key prefix for this channel", example = "tb-")
    private String keyPrefix;

    @Schema(description = "Default price multiplier", example = "1.2")
    private BigDecimal priceMultiplier;

    @Schema(description = "Webhook URL for order notifications")
    private String webhookUrl;

    @Schema(description = "Channel configuration as JSON")
    private String config;
}
