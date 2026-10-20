package com.aipanel.analytics.data.mysql.mapper;

import com.aipanel.analytics.data.mysql.po.UsageStatsDailyPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Daily usage stats mapper
 */
@Mapper
public interface UsageStatsDailyMapper extends BaseMapper<UsageStatsDailyPO> {

    @Select("""
        SELECT
            stat_date,
            SUM(request_count) as requests,
            SUM(total_tokens) as tokens,
            SUM(total_cost) as cost
        FROM t_usage_stats_daily
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY stat_date
        ORDER BY stat_date
        """)
    List<Map<String, Object>> getDailyTimeSeries(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Select("""
        SELECT
            channel_id,
            SUM(total_cost) as revenue
        FROM t_usage_stats_daily
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY channel_id
        ORDER BY revenue DESC
        LIMIT 5
        """)
    List<Map<String, Object>> getTopChannelsByRevenue(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
