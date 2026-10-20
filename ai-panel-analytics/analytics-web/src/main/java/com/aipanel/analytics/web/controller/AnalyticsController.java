package com.aipanel.analytics.web.controller;

import com.aipanel.analytics.api.AnalyticsApi;
import com.aipanel.analytics.api.dto.response.DashboardStatsResponse;
import com.aipanel.analytics.api.dto.response.UsageStatsResponse;
import com.aipanel.analytics.core.service.AnalyticsService;
import com.aipanel.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Analytics controller
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Usage analytics and statistics")
public class AnalyticsController implements AnalyticsApi {

    private final AnalyticsService analyticsService;

    @Override
    @GetMapping("/dashboard")
    @Operation(summary = "Get Dashboard Stats", description = "Get dashboard overview statistics")
    public R<DashboardStatsResponse> getDashboardStats() {
        return R.ok(analyticsService.getDashboardStats());
    }

    @Override
    @GetMapping("/usage")
    @Operation(summary = "Get Usage Stats", description = "Get usage statistics for a date range")
    public R<UsageStatsResponse> getUsageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String granularity) {
        return R.ok(analyticsService.getUsageStats(startDate, endDate, granularity));
    }

    @Override
    @GetMapping("/channels/{channelId}/usage")
    @Operation(summary = "Get Channel Usage Stats", description = "Get usage statistics for a specific channel")
    public R<UsageStatsResponse> getChannelUsageStats(
            @PathVariable Long channelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getChannelUsageStats(channelId, startDate, endDate));
    }

    @Override
    @GetMapping("/keys/{keyId}/usage")
    @Operation(summary = "Get Key Usage Stats", description = "Get usage statistics for a specific API key")
    public R<UsageStatsResponse> getKeyUsageStats(
            @PathVariable String keyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analyticsService.getKeyUsageStats(keyId, startDate, endDate));
    }
}
