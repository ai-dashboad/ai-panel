package com.aipanel.dispatch.data.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_model_group_member")
public class ModelGroupMemberPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private String modelId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
