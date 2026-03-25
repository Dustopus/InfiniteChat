package com.dustopus.infinitechat.auth.vo;

import com.dustopus.infinitechat.common.dto.user.UserDTO;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserDTO user;
}
