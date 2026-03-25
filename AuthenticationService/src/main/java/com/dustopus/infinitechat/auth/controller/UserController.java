package com.dustopus.infinitechat.auth.controller;

import com.dustopus.infinitechat.auth.service.UserService;
import com.dustopus.infinitechat.auth.vo.*;
import com.dustopus.infinitechat.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/sms/send")
    public Result<?> sendSmsCode(@RequestBody @Valid SmsCodeRequest request) {
        userService.sendSmsCode(request.getPhone());
        return Result.ok();
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid RegisterRequest request) {
        Long userId = userService.register(request);
        return Result.ok(userId);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/login/code")
    public Result<LoginResponse> loginByCode(@RequestBody @Valid LoginByCodeRequest request) {
        return Result.ok(userService.loginByCode(request));
    }

    @GetMapping("/info")
    public Result<LoginResponse> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(userService.getUserInfo(userId));
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                    @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("X-User-Id") Long userId) {
        userService.logout(userId);
        return Result.ok();
    }
}
