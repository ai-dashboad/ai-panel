package com.aipanel.channel.data.mysql.mapper;

import com.aipanel.channel.data.mysql.po.UsageLogPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Usage log mapper
 */
@Mapper
public interface UsageLogMapper extends BaseMapper<UsageLogPO> {
}
