package com.dustopus.infinitechat.messaging.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.INPUT)
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String chatType;
    /** 1文本 2图片 3文件 4语音 5视频 6红包 7系统 */
    private Integer msgType;
    private String content;
    private String extra;
    /** 1正常 2撤回 3删除 */
    private Integer status;
    private Long replyTo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
