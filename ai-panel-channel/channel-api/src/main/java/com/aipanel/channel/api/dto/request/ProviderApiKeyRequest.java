package com.aipanel.channel.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider API Key Request DTO
 */
@Data
public class ProviderApiKeyRequest {

    /**
     * Provider ID
     */
    @NotNull(message = "Provider ID is required")
    private Long providerId;

    /**
     * Name/alias for this key
     */
    @NotBlank(message = "Key name is required")
    private String keyName;

    /**
     * API key value
     */
    @NotBlank(message = "API key is required")
    private String apiKey;

    /**
     * Whether this key is enabled
     */
    private Boolean enabled = true;

    /**
     * Weight for load balancing (higher = more traffic)
     */
    private Integer weight = 100;

    /**
     * Rate limit for this specific key (per minute)
     */
    private Integer rateLimitPerMinute;

    /**
     * Daily token quota for this key
     */
    private Long dailyQuota;

    /**
     * Key expiration date
     */
    private LocalDateTime expiresAt;
}
