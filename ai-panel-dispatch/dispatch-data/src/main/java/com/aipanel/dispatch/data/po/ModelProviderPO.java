package com.aipanel.dispatch.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Model Provider Persistence Object
 */
@Data
@TableName("t_model_provider")
public class ModelProviderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Provider code (e.g., openai, anthropic)
     */
    private String providerCode;

    /**
     * Provider name
     */
    private String providerName;

    /**
     * API base URL
     */
    private String baseUrl;

    /**
     * API key (encrypted)
     */
    private String apiKey;

    /**
     * Priority (lower = higher priority)
     */
    private Integer priority;

    /**
     * Rate limit per minute
     */
    private Integer rateLimitPerMinute;

    /**
     * Timeout in milliseconds
     */
    private Integer timeout;

    /**
     * Whether provider is enabled
     */
    private Boolean enabled;

    /**
     * Health status (0=healthy, 1=degraded, 2=unhealthy)
     */
    private Integer healthStatus;

    /**
     * Last health check time
     */
    private LocalDateTime lastHealthCheck;

    /**
     * Additional config JSON
     */
    private String config;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
