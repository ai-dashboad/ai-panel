package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Channel API key persistent object (for reseller keys)
 */
@Data
@TableName("t_channel_api_key")
public class ChannelApiKeyPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("channel_id")
    private Long channelId;

    @TableField("key_id")
    private String keyId;

    @TableField("key_hash")
    private String keyHash;

    @TableField("key_prefix")
    private String keyPrefix;

    @TableField("name")
    private String name;

    @TableField("plan_type")
    private String planType;

    @TableField("monthly_quota")
    private Long monthlyQuota;

    @TableField("used_quota")
    private Long usedQuota;

    @TableField("rate_limit")
    private Integer rateLimit;

    @TableField("allowed_models")
    private String allowedModels;

    @TableField("status")
    private Integer status;

    @TableField("price")
    private BigDecimal price;

    @TableField("sold_at")
    private LocalDateTime soldAt;

    @TableField("buyer_info")
    private String buyerInfo;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
