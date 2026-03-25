package com.dustopus.infinitechat.auth.service;

import com.dustopus.infinitechat.auth.vo.*;

public interface UserService {
    void sendSmsCode(String phone);
    Long register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse loginByCode(LoginByCodeRequest request);
    LoginResponse getUserInfo(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request);
    void logout(Long userId);
}
