package com.dustopus.authenticationservice.data.user.register;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class registerResponse {
    private String phone;
}
