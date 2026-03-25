package com.dustopus.infinitechat.common.dto.user;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {
    private Long userId;
    private String userName;
    private String email;
    private String phone;
    private String avatar;
    private String signature;
    private Integer gender;
    private Integer status;
    private LocalDateTime createdAt;
}
