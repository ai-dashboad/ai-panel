package com.aipanel.model.data.mapper;

import com.aipanel.model.data.po.ModelPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Model Mapper
 */
@Mapper
public interface ModelMapper extends BaseMapper<ModelPO> {

    @Select("SELECT * FROM t_model WHERE model_id = #{modelId} AND deleted = 0")
    ModelPO findByModelId(@Param("modelId") String modelId);

    @Select("SELECT * FROM t_model WHERE provider_code = #{providerCode} AND deleted = 0")
    List<ModelPO> findByProviderCode(@Param("providerCode") String providerCode);

    @Select("SELECT * FROM t_model WHERE model_type = #{modelType} AND deleted = 0")
    List<ModelPO> findByModelType(@Param("modelType") String modelType);

    @Select("SELECT * FROM t_model WHERE enabled = 1 AND deleted = 0 ORDER BY provider_code, model_id")
    List<ModelPO> findAllEnabled();

    @Select("SELECT COUNT(*) FROM t_model WHERE provider_code = #{providerCode} AND deleted = 0")
    int countByProviderCode(@Param("providerCode") String providerCode);
}
