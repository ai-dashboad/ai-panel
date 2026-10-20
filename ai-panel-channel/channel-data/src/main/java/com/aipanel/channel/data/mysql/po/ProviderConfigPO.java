package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider Configuration Entity
 */
@Data
@TableName("provider_config")
public class ProviderConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Provider name for display
     */
    private String name;

    /**
     * Provider type: openai, anthropic, deepseek, moonshot, etc.
     */
    private String type;

    /**
     * Base URL for API calls
     */
    private String baseUrl;

    /**
     * Whether this provider is enabled
     */
    private Boolean enabled;

    /**
     * Priority for fallback routing (lower = higher priority)
     */
    private Integer priority;

    /**
     * Request timeout in seconds
     */
    private Integer timeoutSeconds;

    /**
     * Number of retries on failure
     */
    private Integer retryCount;

    /**
     * Description of this provider
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
