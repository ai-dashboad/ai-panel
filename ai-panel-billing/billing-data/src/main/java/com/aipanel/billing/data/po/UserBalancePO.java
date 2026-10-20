package com.aipanel.billing.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * User Balance Persistence Object
 */
@Data
@TableName("t_user_balance")
public class UserBalancePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private BigDecimal balance;
    private BigDecimal totalCharged;
    private BigDecimal totalUsed;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
