package com.aipanel.dispatch.data.mapper;

import com.aipanel.dispatch.data.po.ModelProviderPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Model Provider Mapper
 */
@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProviderPO> {

    /**
     * Find all enabled providers ordered by priority
     */
    @Select("SELECT * FROM t_model_provider WHERE enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<ModelProviderPO> findAllEnabled();

    /**
     * Find provider by code
     */
    @Select("SELECT * FROM t_model_provider WHERE provider_code = #{providerCode} AND deleted = 0")
    ModelProviderPO findByProviderCode(@Param("providerCode") String providerCode);

    /**
     * Update health status
     */
    @Update("UPDATE t_model_provider SET health_status = #{healthStatus}, last_health_check = NOW() WHERE id = #{id}")
    int updateHealthStatus(@Param("id") Long id, @Param("healthStatus") Integer healthStatus);

    /**
     * Find healthy providers
     */
    @Select("SELECT * FROM t_model_provider WHERE enabled = 1 AND deleted = 0 AND health_status = 0 ORDER BY priority ASC")
    List<ModelProviderPO> findHealthyProviders();
}
