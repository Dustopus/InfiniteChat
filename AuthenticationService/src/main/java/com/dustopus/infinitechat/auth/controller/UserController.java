package com.dustopus.infinitechat.auth.controller;

import com.dustopus.infinitechat.auth.service.UserService;
import com.dustopus.infinitechat.auth.vo.LoginRequest;
import com.dustopus.infinitechat.auth.vo.LoginResponse;
import com.dustopus.infinitechat.auth.vo.RegisterRequest;
import com.dustopus.infinitechat.auth.vo.SmsCodeRequest;
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

    @GetMapping("/info")
    public Result<LoginResponse> getUserInfo(@RequestParam Long userId) {
        return Result.ok(userService.getUserInfo(userId));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestParam Long userId) {
        userService.logout(userId);
        return Result.ok();
    }
}
