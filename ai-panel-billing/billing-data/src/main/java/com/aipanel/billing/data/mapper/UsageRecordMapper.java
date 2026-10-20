package com.aipanel.billing.data.mapper;

import com.aipanel.billing.data.po.UsageRecordPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Usage Record Mapper
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecordPO> {

    @Select("SELECT DATE(create_time) as date, " +
            "COUNT(*) as request_count, " +
            "SUM(total_tokens) as total_tokens, " +
            "SUM(total_cost) as total_cost " +
            "FROM t_usage_record " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getDailyStats(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT model, " +
            "COUNT(*) as request_count, " +
            "SUM(input_tokens) as input_tokens, " +
            "SUM(output_tokens) as output_tokens, " +
            "SUM(total_cost) as total_cost " +
            "FROM t_usage_record " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY model")
    List<Map<String, Object>> getModelStats(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT COUNT(*) as total_requests, " +
            "SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_requests, " +
            "SUM(CASE WHEN status = 'error' THEN 1 ELSE 0 END) as failed_requests, " +
            "SUM(input_tokens) as total_input_tokens, " +
            "SUM(output_tokens) as total_output_tokens, " +
            "SUM(total_tokens) as total_tokens, " +
            "SUM(total_cost) as total_cost, " +
            "AVG(latency_ms) as avg_latency " +
            "FROM t_usage_record " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} AND create_time <= #{endTime}")
    Map<String, Object> getSummaryStats(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
