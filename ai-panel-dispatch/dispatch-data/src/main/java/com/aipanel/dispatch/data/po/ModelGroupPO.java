package com.aipanel.dispatch.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_model_group")
public class ModelGroupPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;

    private String groupName;

    private BigDecimal priceMultiplier;

    private String description;

    private Boolean enabled;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
