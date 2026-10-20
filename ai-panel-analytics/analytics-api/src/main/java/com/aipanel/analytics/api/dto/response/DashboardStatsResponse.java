package com.aipanel.analytics.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard overview statistics")
public class DashboardStatsResponse {

    @Schema(description = "Total revenue")
    private BigDecimal totalRevenue;

    @Schema(description = "Revenue change percentage (vs previous period)")
    private Double revenueChange;

    @Schema(description = "Total active API keys")
    private Long activeKeys;

    @Schema(description = "Active keys change percentage")
    private Double activeKeysChange;

    @Schema(description = "Total requests today")
    private Long requestsToday;

    @Schema(description = "Requests change percentage")
    private Double requestsChange;

    @Schema(description = "Total tokens today")
    private Long tokensToday;

    @Schema(description = "Tokens change percentage")
    private Double tokensChange;

    @Schema(description = "Active channels count")
    private Long activeChannels;

    @Schema(description = "Error rate percentage")
    private Double errorRate;

    @Schema(description = "Average response time in ms")
    private Double avgResponseTime;

    @Schema(description = "Top models by usage")
    private List<TopModel> topModels;

    @Schema(description = "Top channels by revenue")
    private List<TopChannel> topChannels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Top model statistics")
    public static class TopModel {
        @Schema(description = "Model name")
        private String model;

        @Schema(description = "Request count")
        private Long requests;

        @Schema(description = "Percentage of total")
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Top channel statistics")
    public static class TopChannel {
        @Schema(description = "Channel ID")
        private Long channelId;

        @Schema(description = "Channel name")
        private String channelName;

        @Schema(description = "Revenue")
        private BigDecimal revenue;

        @Schema(description = "Percentage of total")
        private Double percentage;
    }
}
