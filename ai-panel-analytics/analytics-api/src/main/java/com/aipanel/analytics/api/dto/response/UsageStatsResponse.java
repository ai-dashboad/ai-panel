package com.aipanel.analytics.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Usage statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Usage statistics")
public class UsageStatsResponse {

    @Schema(description = "Total requests")
    private Long totalRequests;

    @Schema(description = "Successful requests")
    private Long successfulRequests;

    @Schema(description = "Failed requests")
    private Long failedRequests;

    @Schema(description = "Total tokens used")
    private Long totalTokens;

    @Schema(description = "Prompt tokens used")
    private Long promptTokens;

    @Schema(description = "Completion tokens used")
    private Long completionTokens;

    @Schema(description = "Total cost")
    private BigDecimal totalCost;

    @Schema(description = "Average latency in milliseconds")
    private Double averageLatencyMs;

    @Schema(description = "Success rate percentage")
    private Double successRate;

    @Schema(description = "Time series data points")
    private List<TimeSeriesPoint> timeSeries;

    @Schema(description = "Model usage breakdown")
    private List<ModelUsage> modelBreakdown;

    @Schema(description = "Provider usage breakdown")
    private List<ProviderUsage> providerBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Time series data point")
    public static class TimeSeriesPoint {
        @Schema(description = "Timestamp")
        private LocalDateTime timestamp;

        @Schema(description = "Requests count")
        private Long requests;

        @Schema(description = "Tokens used")
        private Long tokens;

        @Schema(description = "Cost")
        private BigDecimal cost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Model usage statistics")
    public static class ModelUsage {
        @Schema(description = "Model name")
        private String model;

        @Schema(description = "Request count")
        private Long requests;

        @Schema(description = "Tokens used")
        private Long tokens;

        @Schema(description = "Cost")
        private BigDecimal cost;

        @Schema(description = "Percentage of total")
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Provider usage statistics")
    public static class ProviderUsage {
        @Schema(description = "Provider name")
        private String provider;

        @Schema(description = "Request count")
        private Long requests;

        @Schema(description = "Success rate")
        private Double successRate;

        @Schema(description = "Average latency")
        private Double averageLatencyMs;
    }
}
