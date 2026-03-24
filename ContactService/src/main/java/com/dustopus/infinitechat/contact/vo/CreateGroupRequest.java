package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotBlank(message = "群名称不能为空")
    private String groupName;
    private String avatar;
}
