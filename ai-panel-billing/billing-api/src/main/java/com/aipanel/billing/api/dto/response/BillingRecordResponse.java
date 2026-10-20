package com.aipanel.billing.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Billing Record Response
 */
@Data
@Builder
@Schema(description = "Billing Record Response")
public class BillingRecordResponse {

    @Schema(description = "Record ID")
    private Long id;

    @Schema(description = "Request ID")
    private String requestId;

    @Schema(description = "Model ID")
    private String model;

    @Schema(description = "Provider code")
    private String provider;

    @Schema(description = "Input tokens")
    private Integer inputTokens;

    @Schema(description = "Output tokens")
    private Integer outputTokens;

    @Schema(description = "Total tokens")
    private Integer totalTokens;

    @Schema(description = "Input cost")
    private BigDecimal inputCost;

    @Schema(description = "Output cost")
    private BigDecimal outputCost;

    @Schema(description = "Total cost")
    private BigDecimal totalCost;

    @Schema(description = "Latency in milliseconds")
    private Long latencyMs;

    @Schema(description = "Request status")
    private String status;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
