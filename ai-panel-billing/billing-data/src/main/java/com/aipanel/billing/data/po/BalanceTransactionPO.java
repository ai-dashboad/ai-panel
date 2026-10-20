package com.aipanel.billing.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance Transaction Persistence Object
 */
@Data
@TableName("t_balance_transaction")
public class BalanceTransactionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * Transaction type: CHARGE, DEDUCT, REFUND
     */
    private String transactionType;

    /**
     * Amount (positive for charge, negative for deduct)
     */
    private BigDecimal amount;

    /**
     * Balance after transaction
     */
    private BigDecimal balanceAfter;

    /**
     * Reference ID (e.g., order ID, request ID)
     */
    private String referenceId;

    /**
     * Description
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
