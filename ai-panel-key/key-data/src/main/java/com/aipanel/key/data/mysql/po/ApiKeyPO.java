package com.aipanel.key.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API Key persistent object
 */
@Data
@TableName("t_api_key")
public class ApiKeyPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("key_id")
    private String keyId;

    @TableField("key_hash")
    private String keyHash;

    @TableField("key_prefix")
    private String keyPrefix;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField("allowed_models")
    private String allowedModels;

    @TableField("rate_limit_override")
    private Integer rateLimitOverride;

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
