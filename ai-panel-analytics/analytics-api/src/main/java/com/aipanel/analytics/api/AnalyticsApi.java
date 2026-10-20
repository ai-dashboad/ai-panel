package com.aipanel.analytics.api;

import com.aipanel.analytics.api.dto.response.DashboardStatsResponse;
import com.aipanel.analytics.api.dto.response.UsageStatsResponse;
import com.aipanel.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Analytics Service Feign Client
 */
@Tag(name = "Analytics", description = "Usage analytics and statistics")
@FeignClient(name = "ai-panel-analytics")
public interface AnalyticsApi {

    @GetMapping("/api/v1/analytics/dashboard")
    @Operation(summary = "Get Dashboard Stats", description = "Get dashboard overview statistics")
    R<DashboardStatsResponse> getDashboardStats();

    @GetMapping("/api/v1/analytics/usage")
    @Operation(summary = "Get Usage Stats", description = "Get usage statistics for a date range")
    R<UsageStatsResponse> getUsageStats(
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @Parameter(description = "Granularity (hourly, daily)") @RequestParam(defaultValue = "daily") String granularity);

    @GetMapping("/api/v1/analytics/channels/{channelId}/usage")
    @Operation(summary = "Get Channel Usage Stats", description = "Get usage statistics for a specific channel")
    R<UsageStatsResponse> getChannelUsageStats(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId,
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @GetMapping("/api/v1/analytics/keys/{keyId}/usage")
    @Operation(summary = "Get Key Usage Stats", description = "Get usage statistics for a specific API key")
    R<UsageStatsResponse> getKeyUsageStats(
        @Parameter(description = "Key ID", required = true) @PathVariable String keyId,
        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
