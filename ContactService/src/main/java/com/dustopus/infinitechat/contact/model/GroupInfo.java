package com.dustopus.infinitechat.contact.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_info")
public class GroupInfo {
    @TableId(type = IdType.INPUT)
    private Long groupId;
    private String groupName;
    private String avatar;
    private Long ownerId;
    private String notice;
    private Integer memberNum;
    /** 状态 1正常 2解散 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
