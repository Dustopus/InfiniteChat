package com.dustopus.infinitechat.offline.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("offline_message")
public class OfflineMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long messageId;
    private String chatType;
    private Long senderId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
