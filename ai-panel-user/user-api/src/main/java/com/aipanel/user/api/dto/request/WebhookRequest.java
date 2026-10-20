package com.aipanel.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Webhook Callback Request from External Platform")
public class WebhookRequest {

    @Schema(description = "External order ID", example = "TB202401010001")
    private String orderId;

    @Schema(description = "External item/product ID", example = "ITEM001")
    private String itemId;

    @Schema(description = "Product SKU", example = "API-PRO-30D")
    private String sku;

    @Schema(description = "Buyer nickname", example = "张三")
    private String buyerNick;

    @Schema(description = "Buyer phone number", example = "13800138000")
    private String buyerPhone;

    @Schema(description = "Buyer email", example = "buyer@example.com")
    private String buyerEmail;

    @Schema(description = "Buyer message/note", example = "请尽快发货")
    private String buyerMessage;

    @Schema(description = "Order quantity", example = "1")
    private Integer quantity = 1;

    @Schema(description = "Order amount", example = "99.00")
    private BigDecimal amount;

    @Schema(description = "Order status", example = "paid")
    private String status;

    @Schema(description = "Event type", example = "order.paid")
    private String event;

    @Schema(description = "Timestamp", example = "1704067200000")
    private Long timestamp;

    @Schema(description = "Signature for verification", example = "abc123def456")
    private String signature;

    @Schema(description = "Raw request body for signature verification")
    private String rawBody;
}
