package com.aipanel.channel.api.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider API Key Response DTO
 */
@Data
public class ProviderApiKeyResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String providerType;
    private String keyName;

    /**
     * Key prefix for display (e.g., sk-****1234)
     */
    private String keyPrefix;

    private Boolean enabled;
    private Integer weight;
    private Integer rateLimitPerMinute;
    private Long dailyQuota;
    private Long usedQuotaToday;
    private Long totalRequests;
    private Long totalTokens;
    private LocalDateTime lastUsedAt;
    private LocalDateTime lastErrorAt;
    private String lastErrorMessage;
    private Integer consecutiveErrors;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
