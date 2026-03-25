package com.dustopus.infinitechat.contact.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    /** 0普通成员 1管理员 2群主 */
    private Integer role;
    private String nickname;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
