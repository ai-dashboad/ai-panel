package com.aipanel.billing.data.mapper;

import com.aipanel.billing.data.po.BalanceTransactionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Balance Transaction Mapper
 */
@Mapper
public interface BalanceTransactionMapper extends BaseMapper<BalanceTransactionPO> {

    @Select("SELECT * FROM t_balance_transaction WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<BalanceTransactionPO> findByUserId(
        @Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
}
