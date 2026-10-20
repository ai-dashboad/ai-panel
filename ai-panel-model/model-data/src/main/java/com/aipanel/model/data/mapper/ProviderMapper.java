package com.aipanel.model.data.mapper;

import com.aipanel.model.data.po.ProviderPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Provider Mapper
 */
@Mapper
public interface ProviderMapper extends BaseMapper<ProviderPO> {

    @Select("SELECT * FROM t_provider WHERE provider_code = #{providerCode} AND deleted = 0")
    ProviderPO findByProviderCode(@Param("providerCode") String providerCode);

    @Select("SELECT * FROM t_provider WHERE enabled = 1 AND deleted = 0 ORDER BY priority ASC")
    List<ProviderPO> findAllEnabled();

    @Update("UPDATE t_provider SET health_status = #{healthStatus}, last_health_check = NOW() WHERE id = #{id}")
    int updateHealthStatus(@Param("id") Long id, @Param("healthStatus") Integer healthStatus);
}
