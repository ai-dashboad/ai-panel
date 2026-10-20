package com.aipanel.analytics.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily usage statistics persistent object
 */
@Data
@TableName("t_usage_stats_daily")
public class UsageStatsDailyPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("channel_id")
    private Long channelId;

    @TableField("key_id")
    private String keyId;

    @TableField("model")
    private String model;

    @TableField("provider")
    private String provider;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("request_count")
    private Long requestCount;

    @TableField("success_count")
    private Long successCount;

    @TableField("error_count")
    private Long errorCount;

    @TableField("prompt_tokens")
    private Long promptTokens;

    @TableField("completion_tokens")
    private Long completionTokens;

    @TableField("total_tokens")
    private Long totalTokens;

    @TableField("total_cost")
    private BigDecimal totalCost;

    @TableField("avg_latency_ms")
    private Double avgLatencyMs;

    @TableField("unique_keys")
    private Long uniqueKeys;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
