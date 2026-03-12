package com.dustopus.authenticationservice.data.user.register;

import lombok.Data;

@Data
public class registerRequest {
    private String phone;

    private String password;

    private String code;
}
