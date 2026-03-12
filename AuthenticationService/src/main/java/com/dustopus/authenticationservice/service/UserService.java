package com.dustopus.authenticationservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dustopus.authenticationservice.data.user.register.registerRequest;
import com.dustopus.authenticationservice.data.user.register.registerResponse;
import com.dustopus.authenticationservice.model.User;

/**
* @author dustopus
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-03-11 15:09:31
*/
public interface UserService extends IService<User> {

    registerResponse register(registerRequest request);
}
