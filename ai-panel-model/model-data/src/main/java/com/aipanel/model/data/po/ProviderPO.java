package com.aipanel.model.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider Persistence Object
 */
@Data
@TableName("t_provider")
public class ProviderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String apiKey;
    private Integer priority;
    private Integer rateLimitPerMinute;
    private Integer timeout;
    private Boolean enabled;
    private Integer healthStatus;
    private LocalDateTime lastHealthCheck;
    private String config;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
