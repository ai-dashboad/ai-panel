package com.aipanel.billing.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Usage Summary Response
 */
@Data
@Builder
@Schema(description = "Usage Summary Response")
public class UsageSummaryResponse {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Total requests")
    private Long totalRequests;

    @Schema(description = "Successful requests")
    private Long successfulRequests;

    @Schema(description = "Failed requests")
    private Long failedRequests;

    @Schema(description = "Total input tokens")
    private Long totalInputTokens;

    @Schema(description = "Total output tokens")
    private Long totalOutputTokens;

    @Schema(description = "Total tokens")
    private Long totalTokens;

    @Schema(description = "Total cost")
    private BigDecimal totalCost;

    @Schema(description = "Average latency in milliseconds")
    private Double averageLatencyMs;

    @Schema(description = "Usage by model")
    private Map<String, ModelUsage> usageByModel;

    @Schema(description = "Usage by day")
    private List<DailyUsage> usageByDay;

    @Data
    @Builder
    @Schema(description = "Model Usage")
    public static class ModelUsage {
        @Schema(description = "Request count")
        private Long requestCount;

        @Schema(description = "Input tokens")
        private Long inputTokens;

        @Schema(description = "Output tokens")
        private Long outputTokens;

        @Schema(description = "Cost")
        private BigDecimal cost;
    }

    @Data
    @Builder
    @Schema(description = "Daily Usage")
    public static class DailyUsage {
        @Schema(description = "Date")
        private String date;

        @Schema(description = "Request count")
        private Long requestCount;

        @Schema(description = "Total tokens")
        private Long totalTokens;

        @Schema(description = "Cost")
        private BigDecimal cost;
    }
}
