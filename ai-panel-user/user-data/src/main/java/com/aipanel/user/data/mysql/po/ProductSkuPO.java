package com.aipanel.user.data.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_sku")
public class ProductSkuPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;
    private String sku;
    private String productName;
    private String quotaType;
    private Long quotaTokens;
    private Integer validityDays;
    private Integer rateLimitPerMinute;
    private BigDecimal price;
    private BigDecimal cost;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
