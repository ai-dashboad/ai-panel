package com.aipanel.analytics.core.service.impl;

import com.aipanel.analytics.api.dto.response.DashboardStatsResponse;
import com.aipanel.analytics.api.dto.response.UsageStatsResponse;
import com.aipanel.analytics.core.service.AnalyticsService;
import com.aipanel.analytics.data.mysql.mapper.UsageStatsDailyMapper;
import com.aipanel.analytics.data.mysql.mapper.UsageStatsHourlyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analytics service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UsageStatsHourlyMapper hourlyMapper;
    private final UsageStatsDailyMapper dailyMapper;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Get today's stats
        Map<String, Object> todayStats = hourlyMapper.getAggregatedStats(
            today.atStartOfDay(),
            today.plusDays(1).atStartOfDay()
        );

        // Get yesterday's stats for comparison
        Map<String, Object> yesterdayStats = hourlyMapper.getAggregatedStats(
            yesterday.atStartOfDay(),
            today.atStartOfDay()
        );

        // Get top models
        List<Map<String, Object>> topModelsData = hourlyMapper.getModelBreakdown(
            today.minusDays(7).atStartOfDay(),
            today.plusDays(1).atStartOfDay()
        );

        // Get top channels
        List<Map<String, Object>> topChannelsData = dailyMapper.getTopChannelsByRevenue(
            today.minusDays(30),
            today
        );

        // Build response
        long requestsToday = getLong(todayStats, "total_requests");
        long requestsYesterday = getLong(yesterdayStats, "total_requests");
        long tokensToday = getLong(todayStats, "total_tokens");
        long tokensYesterday = getLong(yesterdayStats, "total_tokens");

        List<DashboardStatsResponse.TopModel> topModels = new ArrayList<>();
        long totalModelRequests = topModelsData.stream()
            .mapToLong(m -> getLong(m, "requests"))
            .sum();

        for (Map<String, Object> model : topModelsData) {
            long requests = getLong(model, "requests");
            topModels.add(DashboardStatsResponse.TopModel.builder()
                .model(getString(model, "model"))
                .requests(requests)
                .percentage(totalModelRequests > 0 ? (double) requests / totalModelRequests * 100 : 0)
                .build());
        }

        List<DashboardStatsResponse.TopChannel> topChannels = new ArrayList<>();
        BigDecimal totalRevenue = topChannelsData.stream()
            .map(m -> getBigDecimal(m, "revenue"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map<String, Object> channel : topChannelsData) {
            BigDecimal revenue = getBigDecimal(channel, "revenue");
            topChannels.add(DashboardStatsResponse.TopChannel.builder()
                .channelId(getLong(channel, "channel_id"))
                .channelName("Channel " + getLong(channel, "channel_id")) // TODO: Fetch actual name
                .revenue(revenue)
                .percentage(totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).doubleValue()
                    : 0)
                .build());
        }

        long successCount = getLong(todayStats, "success_count");
        long errorCount = getLong(todayStats, "error_count");
        double errorRate = requestsToday > 0 ? (double) errorCount / requestsToday * 100 : 0;

        return DashboardStatsResponse.builder()
            .totalRevenue(totalRevenue)
            .revenueChange(0.0) // TODO: Calculate change
            .activeKeys(0L) // TODO: Query from channel service
            .activeKeysChange(0.0)
            .requestsToday(requestsToday)
            .requestsChange(requestsYesterday > 0
                ? (double) (requestsToday - requestsYesterday) / requestsYesterday * 100 : 0)
            .tokensToday(tokensToday)
            .tokensChange(tokensYesterday > 0
                ? (double) (tokensToday - tokensYesterday) / tokensYesterday * 100 : 0)
            .activeChannels(0L) // TODO: Query from channel service
            .errorRate(errorRate)
            .avgResponseTime(getDouble(todayStats, "avg_latency"))
            .topModels(topModels)
            .topChannels(topChannels)
            .build();
    }

    @Override
    public UsageStatsResponse getUsageStats(LocalDate startDate, LocalDate endDate, String granularity) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        Map<String, Object> stats = hourlyMapper.getAggregatedStats(startTime, endTime);
        List<Map<String, Object>> modelBreakdown = hourlyMapper.getModelBreakdown(startTime, endTime);
        List<Map<String, Object>> timeSeries = dailyMapper.getDailyTimeSeries(startDate, endDate);

        return buildUsageStatsResponse(stats, modelBreakdown, timeSeries);
    }

    @Override
    public UsageStatsResponse getChannelUsageStats(Long channelId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implement channel-specific stats
        return getUsageStats(startDate, endDate, "daily");
    }

    @Override
    public UsageStatsResponse getKeyUsageStats(String keyId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implement key-specific stats
        return getUsageStats(startDate, endDate, "daily");
    }

    private UsageStatsResponse buildUsageStatsResponse(
            Map<String, Object> stats,
            List<Map<String, Object>> modelBreakdown,
            List<Map<String, Object>> timeSeries) {

        long totalRequests = getLong(stats, "total_requests");
        long successCount = getLong(stats, "success_count");

        List<UsageStatsResponse.TimeSeriesPoint> timeSeriesPoints = new ArrayList<>();
        for (Map<String, Object> point : timeSeries) {
            timeSeriesPoints.add(UsageStatsResponse.TimeSeriesPoint.builder()
                .timestamp(((java.sql.Date) point.get("stat_date")).toLocalDate().atStartOfDay())
                .requests(getLong(point, "requests"))
                .tokens(getLong(point, "tokens"))
                .cost(getBigDecimal(point, "cost"))
                .build());
        }

        List<UsageStatsResponse.ModelUsage> modelUsages = new ArrayList<>();
        long totalModelRequests = modelBreakdown.stream()
            .mapToLong(m -> getLong(m, "requests"))
            .sum();

        for (Map<String, Object> model : modelBreakdown) {
            long requests = getLong(model, "requests");
            modelUsages.add(UsageStatsResponse.ModelUsage.builder()
                .model(getString(model, "model"))
                .requests(requests)
                .tokens(getLong(model, "tokens"))
                .cost(getBigDecimal(model, "cost"))
                .percentage(totalModelRequests > 0 ? (double) requests / totalModelRequests * 100 : 0)
                .build());
        }

        return UsageStatsResponse.builder()
            .totalRequests(totalRequests)
            .successfulRequests(successCount)
            .failedRequests(getLong(stats, "error_count"))
            .totalTokens(getLong(stats, "total_tokens"))
            .promptTokens(getLong(stats, "prompt_tokens"))
            .completionTokens(getLong(stats, "completion_tokens"))
            .totalCost(getBigDecimal(stats, "total_cost"))
            .averageLatencyMs(getDouble(stats, "avg_latency"))
            .successRate(totalRequests > 0 ? (double) successCount / totalRequests * 100 : 0)
            .timeSeries(timeSeriesPoints)
            .modelBreakdown(modelUsages)
            .build();
    }

    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return BigDecimal.ZERO;
    }
}
