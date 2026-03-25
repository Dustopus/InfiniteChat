package com.dustopus.infinitechat.auth.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private String userName;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private String signature;
    private Integer gender;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
