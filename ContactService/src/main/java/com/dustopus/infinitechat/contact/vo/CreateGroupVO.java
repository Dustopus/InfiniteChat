package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupVO {
    @NotBlank(message = "群名称不能为空")
    private String groupName;
    @NotNull(message = "成员列表不能为空")
    private List<Long> memberIds;
    private String avatar;
}
