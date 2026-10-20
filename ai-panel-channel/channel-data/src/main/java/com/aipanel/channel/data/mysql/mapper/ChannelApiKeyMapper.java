package com.aipanel.channel.data.mysql.mapper;

import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Channel API key mapper
 */
@Mapper
public interface ChannelApiKeyMapper extends BaseMapper<ChannelApiKeyPO> {

    /**
     * Find API key by hash
     */
    @Select("SELECT * FROM t_channel_api_key WHERE key_hash = #{keyHash} AND deleted = 0")
    ChannelApiKeyPO findByKeyHash(@Param("keyHash") String keyHash);

    /**
     * Find API key by key ID
     */
    @Select("SELECT * FROM t_channel_api_key WHERE key_id = #{keyId} AND deleted = 0")
    ChannelApiKeyPO findByKeyId(@Param("keyId") String keyId);

    /**
     * Increment used quota
     */
    @Select("UPDATE t_channel_api_key SET used_quota = used_quota + #{tokens}, " +
            "last_used_at = NOW(), updated_at = NOW() WHERE key_id = #{keyId}")
    void incrementUsedQuota(@Param("keyId") String keyId, @Param("tokens") long tokens);
}
