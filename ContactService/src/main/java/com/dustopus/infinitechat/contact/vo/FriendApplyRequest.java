package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendApplyRequest {
    @NotNull(message = "对方用户ID不能为空")
    private Long toId;
    private String message;
}
