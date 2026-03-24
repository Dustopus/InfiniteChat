package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendRequestVO {
    @NotNull(message = "被申请人ID不能为空")
    private Long toId;
    private String message;
}
