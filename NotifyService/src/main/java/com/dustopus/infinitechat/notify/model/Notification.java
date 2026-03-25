package com.dustopus.infinitechat.notify.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private Long refId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
