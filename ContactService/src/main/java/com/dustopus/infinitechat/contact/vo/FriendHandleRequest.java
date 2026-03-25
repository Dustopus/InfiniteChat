package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendHandleRequest {
    @NotNull(message = "申请ID不能为空")
    private Long requestId;
    /** 1同意 2拒绝 */
    @NotNull(message = "操作类型不能为空")
    private Integer action;
}
