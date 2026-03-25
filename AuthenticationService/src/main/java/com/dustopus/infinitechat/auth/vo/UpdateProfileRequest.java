package com.dustopus.infinitechat.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String userName;
    private String avatar;
    private String signature;
    /** 性别 0男 1女 2保密 */
    private Integer gender;
}
