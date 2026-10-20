package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API usage log persistent object
 */
@Data
@TableName("t_usage_log")
public class UsageLogPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("channel_id")
    private Long channelId;

    @TableField("key_id")
    private String keyId;

    @TableField("request_id")
    private String requestId;

    @TableField("model")
    private String model;

    @TableField("actual_model")
    private String actualModel;

    @TableField("provider")
    private String provider;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("cost")
    private BigDecimal cost;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
