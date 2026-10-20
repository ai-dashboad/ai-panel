package com.aipanel.dispatch.controller;

import com.aipanel.common.domain.R;
import com.aipanel.dispatch.api.dto.response.ModelMetricsResponse;
import com.aipanel.dispatch.data.service.RequestLogDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statistics Controller
 * Usage analytics and monitoring endpoints
 */
@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "Usage Statistics API")
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final RequestLogDataService requestLogDataService;

    /**
     * Get user daily usage statistics
     */
    @GetMapping("/user/{userId}/daily")
    @Operation(summary = "Get user daily stats", description = "Get daily usage statistics for a user")
    public R<List<Map<String, Object>>> getUserDailyStats(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<Map<String, Object>> stats = requestLogDataService.getUserDailyStats(userId, startTime, endTime);
        return R.ok(stats);
    }

    /**
     * Get model usage statistics
     */
    @GetMapping("/models")
    @Operation(summary = "Get model stats", description = "Get usage statistics by model")
    public R<List<Map<String, Object>>> getModelStats(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);

        List<Map<String, Object>> stats = requestLogDataService.getModelStats(startTime, endTime);
        return R.ok(stats);
    }

    /**
     * Get provider error rates
     */
    @GetMapping("/providers/errors")
    @Operation(summary = "Get provider error rates", description = "Get error rates by provider")
    public R<List<Map<String, Object>>> getProviderErrorRates(
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "24") int hours
    ) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Map<String, Object>> stats = requestLogDataService.getProviderErrorRates(since);
        return R.ok(stats);
    }

    /**
     * Flush pending logs
     */
    @PostMapping("/logs/flush")
    @Operation(summary = "Flush logs", description = "Flush pending request logs to database")
    public R<String> flushLogs() {
        requestLogDataService.flushLogs();
        return R.ok("Logs flushed");
    }

    /**
     * Per-model health metrics for catalog cards (success rate, latency, throughput, sparkline)
     */
    @GetMapping("/models/health")
    @Operation(summary = "Get model health metrics", description = "Success rate, latency, throughput and recent status sparkline per model")
    public R<Map<String, ModelMetricsResponse>> getModelHealthMetrics(
            @Parameter(description = "Hours to look back") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "Sparkline length") @RequestParam(defaultValue = "24") int sparkline
    ) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Map<String, Object>> rows = requestLogDataService.getModelHealthStats(since);
        Map<String, ModelMetricsResponse> result = new HashMap<>();

        for (Map<String, Object> row : rows) {
            String modelId = stringVal(row.get("model_id"));
            if (modelId == null || modelId.isBlank()) {
                continue;
            }
            long requestCount = toLong(row.get("request_count"));
            long successCount = toLong(row.get("success_count"));
            double avgLatencyMs = toDouble(row.get("avg_latency_ms"));
            double totalOutput = toDouble(row.get("total_output_tokens"));
            double totalLatencyMs = toDouble(row.get("total_latency_ms"));

            double successRate = requestCount == 0 ? 100.0 : (successCount * 100.0 / requestCount);
            double avgLatencySec = avgLatencyMs / 1000.0;
            double tps = totalLatencyMs > 0 ? (totalOutput / (totalLatencyMs / 1000.0)) : 0.0;

            List<Integer> recent = requestLogDataService.getRecentStatuses(modelId, sparkline).stream()
                .map(r -> "success".equalsIgnoreCase(stringVal(r.get("status"))) ? 1 : 0)
                .collect(Collectors.toCollection(ArrayList::new));
            // Chronological left-to-right
            java.util.Collections.reverse(recent);

            result.put(modelId, ModelMetricsResponse.builder()
                .modelId(modelId)
                .successRate(round2(successRate))
                .avgLatencySeconds(round2(avgLatencySec))
                .throughputTokensPerSecond(round1(tps))
                .requestCount(requestCount)
                .recentStatuses(recent)
                .build());
        }

        return R.ok(result);
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString();
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
