package com.aipanel.billing.data.mapper;

import com.aipanel.billing.data.po.UserBalancePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * User Balance Mapper
 */
@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalancePO> {

    @Select("SELECT * FROM t_user_balance WHERE user_id = #{userId}")
    UserBalancePO findByUserId(@Param("userId") Long userId);

    @Update("UPDATE t_user_balance SET balance = balance + #{amount}, " +
            "total_charged = total_charged + #{amount}, " +
            "version = version + 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version}")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    @Update("UPDATE t_user_balance SET balance = balance - #{amount}, " +
            "total_used = total_used + #{amount}, " +
            "version = version + 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND version = #{version} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
}
