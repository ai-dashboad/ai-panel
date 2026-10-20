package com.aipanel.dispatch.data.mapper;

import com.aipanel.dispatch.data.po.ModelGroupMemberPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelGroupMemberMapper extends BaseMapper<ModelGroupMemberPO> {

    @Select("SELECT * FROM t_model_group_member WHERE group_id = #{groupId}")
    List<ModelGroupMemberPO> findByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT * FROM t_model_group_member WHERE model_id = #{modelId}")
    List<ModelGroupMemberPO> findByModelId(@Param("modelId") String modelId);

    @Select("SELECT m.* FROM t_model_group_member m " +
            "INNER JOIN t_model_group g ON g.id = m.group_id AND g.deleted = 0 AND g.enabled = 1 " +
            "WHERE m.model_id = #{modelId} ORDER BY g.sort_order ASC LIMIT 1")
    ModelGroupMemberPO findPrimaryMembership(@Param("modelId") String modelId);
}
