package com.aipanel.channel.data.mysql.mapper;

import com.aipanel.channel.data.mysql.po.ProviderModelPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Provider Model Mapper
 */
@Mapper
public interface ProviderModelMapper extends BaseMapper<ProviderModelPO> {

    /**
     * Physically delete a model row. Logic delete would collide with the
     * (provider_id, model_id, deleted) unique key when the same model is removed twice.
     */
    @Delete("DELETE FROM provider_model WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);

    /**
     * Physically delete all models of a provider.
     */
    @Delete("DELETE FROM provider_model WHERE provider_id = #{providerId}")
    int hardDeleteByProviderId(@Param("providerId") Long providerId);
}
