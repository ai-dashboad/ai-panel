package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "External Order Response")
public class ExternalOrderResponse {

    @Schema(description = "Order ID", example = "1")
    private Long id;

    @Schema(description = "Platform identifier", example = "taobao")
    private String platform;

    @Schema(description = "External order ID", example = "TB202401010001")
    private String externalOrderId;

    @Schema(description = "External item ID", example = "ITEM001")
    private String externalItemId;

    @Schema(description = "Product SKU", example = "API-PRO-30D")
    private String sku;

    @Schema(description = "Buyer nickname", example = "张三")
    private String buyerNick;

    @Schema(description = "Buyer phone (masked)", example = "138****8000")
    private String buyerPhone;

    @Schema(description = "Buyer email", example = "buyer@example.com")
    private String buyerEmail;

    @Schema(description = "Buyer message")
    private String buyerMessage;

    @Schema(description = "Order quantity", example = "1")
    private Integer quantity;

    @Schema(description = "Order amount", example = "99.00")
    private BigDecimal amount;

    @Schema(description = "Associated user ID")
    private Long userId;

    @Schema(description = "Associated API key ID")
    private Long apiKeyId;

    @Schema(description = "Order status: pending, processing, delivered, failed, cancelled")
    private String status;

    @Schema(description = "Delivery method: sms, email, platform_message")
    private String deliveryMethod;

    @Schema(description = "Delivery content sent to buyer")
    private String deliveryContent;

    @Schema(description = "Delivery time")
    private LocalDateTime deliveryTime;

    @Schema(description = "Failure reason if status is failed")
    private String failReason;

    @Schema(description = "Retry count", example = "0")
    private Integer retryCount;

    @Schema(description = "Order creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
