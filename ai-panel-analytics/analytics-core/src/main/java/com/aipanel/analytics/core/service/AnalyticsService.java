package com.aipanel.analytics.core.service;

import com.aipanel.analytics.api.dto.response.DashboardStatsResponse;
import com.aipanel.analytics.api.dto.response.UsageStatsResponse;

import java.time.LocalDate;

/**
 * Analytics service interface
 */
public interface AnalyticsService {

    /**
     * Get dashboard overview statistics
     */
    DashboardStatsResponse getDashboardStats();

    /**
     * Get usage statistics for a date range
     */
    UsageStatsResponse getUsageStats(LocalDate startDate, LocalDate endDate, String granularity);

    /**
     * Get usage statistics for a specific channel
     */
    UsageStatsResponse getChannelUsageStats(Long channelId, LocalDate startDate, LocalDate endDate);

    /**
     * Get usage statistics for a specific API key
     */
    UsageStatsResponse getKeyUsageStats(String keyId, LocalDate startDate, LocalDate endDate);
}
