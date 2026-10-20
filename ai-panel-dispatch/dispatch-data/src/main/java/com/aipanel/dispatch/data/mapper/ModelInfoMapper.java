package com.aipanel.dispatch.data.mapper;

import com.aipanel.dispatch.data.po.ModelInfoPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Model Info Mapper
 */
@Mapper
public interface ModelInfoMapper extends BaseMapper<ModelInfoPO> {

    /**
     * Find model by model ID
     */
    @Select("SELECT * FROM t_model_info WHERE model_id = #{modelId} AND deleted = 0")
    ModelInfoPO findByModelId(@Param("modelId") String modelId);

    /**
     * Find all enabled models
     */
    @Select("SELECT * FROM t_model_info WHERE enabled = 1 AND deleted = 0 ORDER BY provider_code, model_id")
    List<ModelInfoPO> findAllEnabled();

    /**
     * Find models by provider
     */
    @Select("SELECT * FROM t_model_info WHERE provider_code = #{providerCode} AND enabled = 1 AND deleted = 0")
    List<ModelInfoPO> findByProviderCode(@Param("providerCode") String providerCode);

    /**
     * Find models by type
     */
    @Select("SELECT * FROM t_model_info WHERE model_type = #{modelType} AND enabled = 1 AND deleted = 0")
    List<ModelInfoPO> findByModelType(@Param("modelType") String modelType);

    /**
     * Search models by keyword
     */
    @Select("SELECT * FROM t_model_info WHERE (model_id LIKE CONCAT('%', #{keyword}, '%') OR display_name LIKE CONCAT('%', #{keyword}, '%')) AND enabled = 1 AND deleted = 0")
    List<ModelInfoPO> searchModels(@Param("keyword") String keyword);
}
