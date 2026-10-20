package com.aipanel.analytics.core.aggregator;

import com.aipanel.analytics.data.mysql.mapper.UsageStatsDailyMapper;
import com.aipanel.analytics.data.mysql.mapper.UsageStatsHourlyMapper;
import com.aipanel.analytics.data.mysql.po.UsageStatsDailyPO;
import com.aipanel.analytics.data.mysql.po.UsageStatsHourlyPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduled task for aggregating usage statistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsAggregator {

    private final UsageStatsHourlyMapper hourlyMapper;
    private final UsageStatsDailyMapper dailyMapper;

    /**
     * Aggregate hourly stats from usage logs
     * Runs every hour at minute 5
     */
    @Scheduled(cron = "0 5 * * * *")
    public void aggregateHourlyStats() {
        log.info("Starting hourly stats aggregation");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourStart = now.withMinute(0).withSecond(0).withNano(0).minusHours(1);

        // TODO: Implement aggregation from usage_log table
        // Group by channel_id, key_id, model, provider for the past hour
        // Insert/update into t_usage_stats_hourly

        log.info("Completed hourly stats aggregation for hour: {}", hourStart);
    }

    /**
     * Aggregate daily stats from hourly stats
     * Runs every day at 00:30
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void aggregateDailyStats() {
        log.info("Starting daily stats aggregation");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // TODO: Implement aggregation from hourly stats
        // Group by channel_id, key_id, model, provider for yesterday
        // Insert/update into t_usage_stats_daily

        log.info("Completed daily stats aggregation for date: {}", yesterday);
    }

    /**
     * Clean up old hourly stats (keep 7 days)
     * Runs every day at 03:00
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldHourlyStats() {
        log.info("Starting hourly stats cleanup");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        // TODO: Delete hourly stats older than 7 days

        log.info("Completed hourly stats cleanup");
    }
}
