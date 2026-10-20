package com.aipanel.dispatch.data.mapper;

import com.aipanel.dispatch.data.po.RequestLogPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request Log Mapper
 */
@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLogPO> {

    /**
     * Batch insert request logs
     */
    @Insert({
        "<script>",
        "INSERT INTO t_request_log (request_id, user_id, api_key_id, requested_model, actual_model, ",
        "provider_code, request_type, input_tokens, output_tokens, total_tokens, latency_ms, ",
        "status, error_message, client_ip, user_agent, fallback_used, retry_count, create_time) VALUES ",
        "<foreach collection='logs' item='log' separator=','>",
        "(#{log.requestId}, #{log.userId}, #{log.apiKeyId}, #{log.requestedModel}, #{log.actualModel}, ",
        "#{log.providerCode}, #{log.requestType}, #{log.inputTokens}, #{log.outputTokens}, #{log.totalTokens}, ",
        "#{log.latencyMs}, #{log.status}, #{log.errorMessage}, #{log.clientIp}, #{log.userAgent}, ",
        "#{log.fallbackUsed}, #{log.retryCount}, #{log.createTime})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("logs") List<RequestLogPO> logs);

    /**
     * Get usage statistics by user
     */
    @Select("SELECT DATE(create_time) as date, SUM(total_tokens) as total_tokens, COUNT(*) as request_count " +
            "FROM t_request_log WHERE user_id = #{userId} AND create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getUserDailyStats(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Get usage statistics by model
     */
    @Select("SELECT actual_model, SUM(total_tokens) as total_tokens, COUNT(*) as request_count, " +
            "AVG(latency_ms) as avg_latency FROM t_request_log " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "GROUP BY actual_model ORDER BY total_tokens DESC")
    List<Map<String, Object>> getModelStats(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Get error rate by provider
     */
    @Select("SELECT provider_code, COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'error' THEN 1 ELSE 0 END) as errors " +
            "FROM t_request_log WHERE create_time >= #{startTime} " +
            "GROUP BY provider_code")
    List<Map<String, Object>> getProviderErrorRates(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT COALESCE(actual_model, requested_model) as model_id, " +
            "COUNT(*) as request_count, " +
            "SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as success_count, " +
            "AVG(latency_ms) as avg_latency_ms, " +
            "SUM(COALESCE(output_tokens, 0)) as total_output_tokens, " +
            "SUM(COALESCE(latency_ms, 0)) as total_latency_ms " +
            "FROM t_request_log WHERE create_time >= #{startTime} " +
            "GROUP BY COALESCE(actual_model, requested_model)")
    List<Map<String, Object>> getModelHealthStats(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT COALESCE(actual_model, requested_model) as model_id, status " +
            "FROM t_request_log " +
            "WHERE COALESCE(actual_model, requested_model) = #{modelId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getRecentStatuses(
            @Param("modelId") String modelId,
            @Param("limit") int limit);
}
