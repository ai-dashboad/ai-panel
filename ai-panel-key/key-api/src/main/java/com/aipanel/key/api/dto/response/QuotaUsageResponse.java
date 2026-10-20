package com.aipanel.key.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Quota and usage statistics response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quota and Usage Statistics Response")
public class QuotaUsageResponse {

    @Schema(description = "API Key ID", example = "key_abc12345")
    private String keyId;

    @Schema(description = "Current period start date", example = "2025-01-01")
    private LocalDate periodStart;

    @Schema(description = "Current period end date", example = "2025-01-31")
    private LocalDate periodEnd;

    @Schema(description = "Requests limit for current period", example = "10000")
    private Long requestsLimit;

    @Schema(description = "Requests used in current period", example = "5000")
    private Long requestsUsed;

    @Schema(description = "Remaining requests", example = "5000")
    private Long requestsRemaining;

    @Schema(description = "Tokens limit for current period", example = "1000000")
    private Long tokensLimit;

    @Schema(description = "Tokens used in current period", example = "500000")
    private Long tokensUsed;

    @Schema(description = "Remaining tokens", example = "500000")
    private Long tokensRemaining;

    @Schema(description = "Total input tokens", example = "200000")
    private Long inputTokens;

    @Schema(description = "Total output tokens", example = "300000")
    private Long outputTokens;

    @Schema(description = "Total cost in USD", example = "15.50")
    private BigDecimal totalCost;

    @Schema(description = "Usage breakdown by model")
    private Map<String, ModelUsage> modelUsage;

    @Schema(description = "Daily usage statistics")
    private List<DailyUsage> dailyUsage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Model usage statistics")
    public static class ModelUsage {
        @Schema(description = "Model name", example = "gpt-4")
        private String model;

        @Schema(description = "Request count", example = "100")
        private Long requests;

        @Schema(description = "Input tokens", example = "50000")
        private Long inputTokens;

        @Schema(description = "Output tokens", example = "100000")
        private Long outputTokens;

        @Schema(description = "Cost in USD", example = "5.00")
        private BigDecimal cost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Daily usage statistics")
    public static class DailyUsage {
        @Schema(description = "Date", example = "2025-01-15")
        private LocalDate date;

        @Schema(description = "Request count", example = "50")
        private Long requests;

        @Schema(description = "Total tokens", example = "10000")
        private Long tokens;

        @Schema(description = "Cost in USD", example = "1.00")
        private BigDecimal cost;
    }
}
