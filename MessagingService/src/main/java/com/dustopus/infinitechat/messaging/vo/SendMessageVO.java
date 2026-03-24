package com.dustopus.infinitechat.messaging.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageVO {
    private Long receiverId;
    private Long groupId;
    @NotBlank(message = "聊天类型不能为空")
    private String chatType;
    @NotNull(message = "消息类型不能为空")
    private Integer msgType;
    @NotBlank(message = "消息内容不能为空")
    private String content;
    private String extra;
    private Long replyTo;
}
