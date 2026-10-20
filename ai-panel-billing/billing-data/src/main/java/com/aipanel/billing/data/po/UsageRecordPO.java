package com.aipanel.billing.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Usage Record Persistence Object
 * Sharded by user_id and month
 */
@Data
@TableName("t_usage_record")
public class UsageRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long apiKeyId;
    private String requestId;
    private String model;
    private String provider;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal inputCost;
    private BigDecimal outputCost;
    private BigDecimal totalCost;
    private Long latencyMs;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
