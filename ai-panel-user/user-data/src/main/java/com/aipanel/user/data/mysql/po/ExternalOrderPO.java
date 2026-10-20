package com.aipanel.user.data.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("external_order")
public class ExternalOrderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;
    private String externalOrderId;
    private String externalItemId;
    private String sku;
    private String buyerNick;
    private String buyerPhone;
    private String buyerEmail;
    private String buyerMessage;
    private Integer quantity;
    private BigDecimal amount;
    private Long userId;
    private Long apiKeyId;
    private String status;
    private String deliveryMethod;
    private String deliveryContent;
    private LocalDateTime deliveryTime;
    private String failReason;
    private Integer retryCount;
    private String rawData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Status constants
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";
}
