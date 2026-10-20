package com.aipanel.channel.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Channel response DTO
 */
@Data
@Schema(description = "Channel information")
public class ChannelResponse {

    @Schema(description = "Channel ID")
    private Long id;

    @Schema(description = "Channel name")
    private String name;

    @Schema(description = "Channel type")
    private String type;

    @Schema(description = "Channel description")
    private String description;

    @Schema(description = "API key prefix")
    private String keyPrefix;

    @Schema(description = "Price multiplier")
    private BigDecimal priceMultiplier;

    @Schema(description = "Webhook URL")
    private String webhookUrl;

    @Schema(description = "Channel status (0: disabled, 1: enabled)")
    private Integer status;

    @Schema(description = "Total keys generated")
    private Long totalKeys;

    @Schema(description = "Active keys count")
    private Long activeKeys;

    @Schema(description = "Total revenue")
    private BigDecimal totalRevenue;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
