package com.aipanel.analytics.data.mysql.mapper;

import com.aipanel.analytics.data.mysql.po.UsageStatsHourlyPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Hourly usage stats mapper
 */
@Mapper
public interface UsageStatsHourlyMapper extends BaseMapper<UsageStatsHourlyPO> {

    @Select("""
        SELECT
            SUM(request_count) as total_requests,
            SUM(success_count) as success_count,
            SUM(error_count) as error_count,
            SUM(total_tokens) as total_tokens,
            SUM(prompt_tokens) as prompt_tokens,
            SUM(completion_tokens) as completion_tokens,
            SUM(total_cost) as total_cost,
            AVG(avg_latency_ms) as avg_latency
        FROM t_usage_stats_hourly
        WHERE stat_hour >= #{startTime} AND stat_hour < #{endTime}
        """)
    Map<String, Object> getAggregatedStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    @Select("""
        SELECT
            model,
            SUM(request_count) as requests,
            SUM(total_tokens) as tokens,
            SUM(total_cost) as cost
        FROM t_usage_stats_hourly
        WHERE stat_hour >= #{startTime} AND stat_hour < #{endTime}
        GROUP BY model
        ORDER BY requests DESC
        LIMIT 10
        """)
    List<Map<String, Object>> getModelBreakdown(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);
}
