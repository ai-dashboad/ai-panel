package com.aipanel.key.data.mysql.mapper;

import com.aipanel.key.data.mysql.po.ApiKeyPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * API Key mapper interface
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKeyPO> {

    @Select("SELECT * FROM t_api_key WHERE key_id = #{keyId} AND deleted = 0")
    ApiKeyPO findByKeyId(@Param("keyId") String keyId);

    @Select("SELECT * FROM t_api_key WHERE key_hash = #{keyHash} AND deleted = 0")
    ApiKeyPO findByKeyHash(@Param("keyHash") String keyHash);

    @Select("SELECT * FROM t_api_key WHERE key_id = #{keyId} AND user_id = #{userId} AND deleted = 0")
    ApiKeyPO findByKeyIdAndUserId(@Param("keyId") String keyId, @Param("userId") Long userId);

    @Select("SELECT * FROM t_api_key WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    IPage<ApiKeyPO> findPageByUserId(Page<ApiKeyPO> page, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM t_api_key WHERE user_id = #{userId} AND deleted = 0")
    long countByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_api_key SET last_used_at = #{lastUsedAt}, updated_at = #{lastUsedAt} WHERE key_id = #{keyId}")
    int updateLastUsed(@Param("keyId") String keyId, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
