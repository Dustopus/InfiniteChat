package com.dustopus.infinitechat.auth.service;

import com.dustopus.infinitechat.auth.vo.LoginResponse;
import com.dustopus.infinitechat.auth.vo.RegisterRequest;
import com.dustopus.infinitechat.auth.vo.LoginRequest;

public interface UserService {
    void sendSmsCode(String phone);
    Long register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse getUserInfo(Long userId);
    void logout(Long userId);
}
