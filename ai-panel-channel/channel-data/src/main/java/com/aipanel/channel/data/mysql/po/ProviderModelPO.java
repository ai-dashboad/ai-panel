package com.aipanel.channel.data.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Provider Model Configuration Entity
 */
@Data
@TableName("provider_model")
public class ProviderModelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Reference to provider_config.id
     */
    private Long providerId;

    /**
     * Model identifier (e.g., gpt-4, claude-3-opus)
     */
    private String modelId;

    /**
     * Display name
     */
    private String modelName;

    /**
     * Whether this model is enabled
     */
    private Boolean enabled;

    /**
     * Input token price per million tokens (USD)
     */
    private BigDecimal inputPricePerMillion;

    /**
     * Output token price per million tokens (USD)
     */
    private BigDecimal outputPricePerMillion;

    /**
     * Maximum tokens for this model
     */
    private Integer maxTokens;

    /**
     * Context window size
     */
    private Integer contextWindow;

    /**
     * Whether model supports vision/images
     */
    private Boolean supportsVision;

    /**
     * Whether model supports function calling
     */
    private Boolean supportsFunctionCall;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
