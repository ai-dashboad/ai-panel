package com.aipanel.channel.data.mysql.mapper;

import com.aipanel.channel.data.mysql.po.ProviderApiKeyPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Provider API Key Mapper
 */
@Mapper
public interface ProviderApiKeyMapper extends BaseMapper<ProviderApiKeyPO> {

    /**
     * Increment usage statistics for a key
     */
    @Update("UPDATE provider_api_key SET " +
            "total_requests = total_requests + 1, " +
            "total_tokens = total_tokens + #{tokens}, " +
            "used_quota_today = used_quota_today + #{tokens}, " +
            "last_used_at = NOW(), " +
            "consecutive_errors = 0, " +
            "status = 'active' " +
            "WHERE id = #{keyId}")
    int incrementUsage(@Param("keyId") Long keyId, @Param("tokens") Long tokens);

    /**
     * Record an error for a key
     */
    @Update("UPDATE provider_api_key SET " +
            "consecutive_errors = consecutive_errors + 1, " +
            "last_error_at = NOW(), " +
            "last_error_message = #{errorMessage}, " +
            "status = CASE WHEN consecutive_errors >= 5 THEN 'error' ELSE status END " +
            "WHERE id = #{keyId}")
    int recordError(@Param("keyId") Long keyId, @Param("errorMessage") String errorMessage);

    /**
     * Reset daily quota for all keys
     */
    @Update("UPDATE provider_api_key SET used_quota_today = 0 WHERE deleted = 0")
    int resetDailyQuota();
}
