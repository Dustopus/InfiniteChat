package com.dustopus.infinitechat.contact.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class AddGroupMembersRequest {
    private Long groupId;
    @NotEmpty(message = "成员列表不能为空")
    private List<Long> userIds;
}
