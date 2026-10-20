package com.aipanel.user.data.mysql.mapper;

import com.aipanel.user.data.mysql.po.UserPlanPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User plan mapper interface
 */
@Mapper
public interface UserPlanMapper extends BaseMapper<UserPlanPO> {

    @Select("SELECT * FROM t_user_plan WHERE user_id = #{userId} AND is_active = 1 " +
            "ORDER BY start_date DESC LIMIT 1")
    UserPlanPO findActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_user_plan WHERE user_id = #{userId} ORDER BY start_date DESC")
    java.util.List<UserPlanPO> findAllByUserId(@Param("userId") Long userId);
}
