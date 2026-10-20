package com.aipanel.dispatch.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Per-model health and performance metrics")
public class ModelMetricsResponse {

    private String modelId;
    private Double successRate;
    private Double avgLatencySeconds;
    private Double throughputTokensPerSecond;
    private Long requestCount;
    /** Recent request statuses: 1=success, 0=error */
    private List<Integer> recentStatuses;
}
