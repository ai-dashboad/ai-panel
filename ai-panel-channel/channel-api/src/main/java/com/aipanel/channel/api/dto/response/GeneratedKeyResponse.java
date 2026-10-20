package com.aipanel.channel.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Generated API key response
 */
@Data
@Schema(description = "Generated API key information")
public class GeneratedKeyResponse {

    @Schema(description = "Key ID")
    private String keyId;

    @Schema(description = "Full API key (only shown once)")
    private String apiKey;

    @Schema(description = "Key prefix for identification")
    private String keyPrefix;

    @Schema(description = "Key name")
    private String name;

    @Schema(description = "Plan type")
    private String planType;

    @Schema(description = "Monthly quota")
    private Long monthlyQuota;

    @Schema(description = "Rate limit")
    private Integer rateLimit;

    @Schema(description = "Expiration date")
    private LocalDateTime expiresAt;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
