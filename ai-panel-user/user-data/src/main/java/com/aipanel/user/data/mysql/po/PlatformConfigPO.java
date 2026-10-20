package com.aipanel.user.data.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform_config")
public class PlatformConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;
    private String name;
    private String appKey;
    private String appSecret;
    private String webhookSecret;
    private String webhookUrl;
    private String deliveryTemplate;
    private Boolean autoCreateUser;
    private Boolean autoDelivery;
    private Boolean enabled;
    private String config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
