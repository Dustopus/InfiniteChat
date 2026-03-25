package com.dustopus.infinitechat.common.dto.message;

import lombok.Data;
import java.io.Serializable;

@Data
public class MessageDTO implements Serializable {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private Long groupId;
    private String chatType;
    private Integer msgType;
    private String content;
    private String extra;
    private Long timestamp;
    private Long replyTo;
}
