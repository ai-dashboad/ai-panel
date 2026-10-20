package com.aipanel.user.data.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_log")
public class DeliveryLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String action;
    private String status;
    private String message;
    private LocalDateTime createdAt;

    // Action constants
    public static final String ACTION_CREATE_USER = "create_user";
    public static final String ACTION_GENERATE_KEY = "generate_key";
    public static final String ACTION_SEND_DELIVERY = "send_delivery";
    public static final String ACTION_RETRY = "retry";

    // Status constants
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
}
