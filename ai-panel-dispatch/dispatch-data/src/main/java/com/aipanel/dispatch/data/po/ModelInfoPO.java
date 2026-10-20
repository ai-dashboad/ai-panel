package com.aipanel.dispatch.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model Information Persistence Object
 */
@Data
@TableName("t_model_info")
public class ModelInfoPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Model ID (e.g., gpt-4, claude-3-opus)
     */
    private String modelId;

    /**
     * Display name
     */
    private String displayName;

    /**
     * Provider code
     */
    private String providerCode;

    /**
     * Model type (CHAT, EMBEDDING, IMAGE_GENERATION, etc.)
     */
    private String modelType;

    /**
     * Context window size
     */
    private Integer contextWindow;

    /**
     * Max output tokens
     */
    private Integer maxOutputTokens;

    /**
     * Input price per 1K tokens
     */
    private BigDecimal inputPrice;

    /**
     * Output price per 1K tokens
     */
    private BigDecimal outputPrice;

    /**
     * Supported features JSON (e.g., ["vision", "function_calling"])
     */
    private String features;

    /**
     * Fallback chain JSON (e.g., ["gpt-4", "claude-3-opus"])
     */
    private String fallbackChain;

    /**
     * Whether model is enabled
     */
    private Boolean enabled;

    /**
     * Whether model is deprecated
     */
    private Boolean deprecated;

    /**
     * Model description
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
