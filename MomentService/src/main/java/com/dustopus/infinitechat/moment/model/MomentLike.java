package com.dustopus.infinitechat.moment.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("moment_like")
public class MomentLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long momentId;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
