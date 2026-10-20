package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider API Key Pool Entity
 */
@Data
@TableName("provider_api_key")
public class ProviderApiKeyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Reference to provider_config.id
     */
    private Long providerId;

    /**
     * Name/alias for this key
     */
    private String keyName;

    /**
     * Encrypted API key
     */
    private String apiKey;

    /**
     * Key prefix for display (e.g., sk-****1234)
     */
    private String keyPrefix;

    /**
     * Whether this key is enabled
     */
    private Boolean enabled;

    /**
     * Weight for load balancing (higher = more traffic)
     */
    private Integer weight;

    /**
     * Rate limit for this specific key
     */
    private Integer rateLimitPerMinute;

    /**
     * Daily token quota for this key
     */
    private Long dailyQuota;

    /**
     * Tokens used today
     */
    private Long usedQuotaToday;

    /**
     * Total API requests made with this key
     */
    private Long totalRequests;

    /**
     * Total tokens consumed by this key
     */
    private Long totalTokens;

    /**
     * Last time this key was used
     */
    private LocalDateTime lastUsedAt;

    /**
     * Last time this key encountered an error
     */
    private LocalDateTime lastErrorAt;

    /**
     * Last error message
     */
    private String lastErrorMessage;

    /**
     * Consecutive error count (for circuit breaker)
     */
    private Integer consecutiveErrors;

    /**
     * Status: active, rate_limited, error, disabled
     */
    private String status;

    /**
     * Key expiration date
     */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
