package com.aipanel.dispatch.data.mapper;

import com.aipanel.dispatch.data.po.ModelGroupPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelGroupMapper extends BaseMapper<ModelGroupPO> {

    @Select("SELECT * FROM t_model_group WHERE enabled = 1 AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<ModelGroupPO> findAllEnabled();
}
