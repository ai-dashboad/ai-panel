package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sales channel persistent object
 */
@Data
@TableName("t_channel")
public class ChannelPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("description")
    private String description;

    @TableField("key_prefix")
    private String keyPrefix;

    @TableField("price_multiplier")
    private BigDecimal priceMultiplier;

    @TableField("webhook_url")
    private String webhookUrl;

    @TableField("config")
    private String config;

    @TableField("status")
    private Integer status;

    @TableField("total_keys")
    private Long totalKeys;

    @TableField("active_keys")
    private Long activeKeys;

    @TableField("total_revenue")
    private BigDecimal totalRevenue;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
