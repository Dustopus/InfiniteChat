package com.dustopus.authenticationservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dustopus.authenticationservice.data.user.register.registerRequest;
import com.dustopus.authenticationservice.data.user.register.registerResponse;
import com.dustopus.authenticationservice.model.User;
import com.dustopus.authenticationservice.service.UserService;
import com.dustopus.authenticationservice.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author dustopus
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-03-11 15:09:31
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public registerResponse register(registerRequest request) {
        String phone = request.getPhone();

        boolean register = isRegister(phone);


        // check if redis code == rediscode

    }

    private boolean isRegister(String phone){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone",phone);
        long count = this.count(queryWrapper);

        return count > 0;
    }
}




