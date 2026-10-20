package com.aipanel.dispatch.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request Log Persistence Object
 * Tracks all AI API requests for billing and analytics
 */
@Data
@TableName("t_request_log")
public class RequestLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Unique request ID
     */
    private String requestId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * API Key ID
     */
    private Long apiKeyId;

    /**
     * Requested model
     */
    private String requestedModel;

    /**
     * Actual model used (may differ due to fallback)
     */
    private String actualModel;

    /**
     * Provider code
     */
    private String providerCode;

    /**
     * Request type (chat, stream, embedding, etc.)
     */
    private String requestType;

    /**
     * Input tokens
     */
    private Integer inputTokens;

    /**
     * Output tokens
     */
    private Integer outputTokens;

    /**
     * Total tokens
     */
    private Integer totalTokens;

    /**
     * Response latency in milliseconds
     */
    private Long latencyMs;

    /**
     * Request status (success, error, timeout)
     */
    private String status;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Client IP address
     */
    private String clientIp;

    /**
     * User agent
     */
    private String userAgent;

    /**
     * Whether fallback was used
     */
    private Boolean fallbackUsed;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
