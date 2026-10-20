package com.aipanel.model.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model Persistence Object
 */
@Data
@TableName("t_model")
public class ModelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String modelId;
    private String displayName;
    private String providerCode;
    private String modelType;
    private Integer contextWindow;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String features;
    private String fallbackChain;
    private Boolean enabled;
    private Boolean deprecated;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
