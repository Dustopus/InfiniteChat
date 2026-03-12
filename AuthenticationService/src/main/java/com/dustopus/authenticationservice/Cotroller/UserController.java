package com.dustopus.authenticationservice.Cotroller;

import com.dustopus.authenticationservice.common.result;
import com.dustopus.authenticationservice.data.user.register.registerRequest;
import com.dustopus.authenticationservice.data.user.register.registerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: 用户类路由
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
public class  UserController {

    @PostMapping("/register")
    public result<registerResponse> register(@RequestBody registerRequest request){
        registerResponse response = new registerResponse();

        return result.ok(response);

    }
}
