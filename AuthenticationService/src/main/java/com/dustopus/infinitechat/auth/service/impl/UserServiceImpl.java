package com.dustopus.infinitechat.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dustopus.infinitechat.auth.mapper.UserMapper;
import com.dustopus.infinitechat.auth.model.User;
import com.dustopus.infinitechat.auth.service.UserService;
import com.dustopus.infinitechat.auth.vo.LoginRequest;
import com.dustopus.infinitechat.auth.vo.LoginResponse;
import com.dustopus.infinitechat.auth.vo.RegisterRequest;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.user.UserDTO;
import com.dustopus.infinitechat.common.exception.BusinessException;
import com.dustopus.infinitechat.common.jwt.JwtUtil;
import com.dustopus.infinitechat.common.result.ErrorCode;
import com.dustopus.infinitechat.common.snowflake.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public void sendSmsCode(String phone) {
        // Rate limit check
        String rateKey = RedisConstants.RATE_LIMIT_PREFIX + "sms:" + phone;
        Boolean limited = stringRedisTemplate.hasKey(rateKey);
        if (Boolean.TRUE.equals(limited)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "验证码发送过于频繁，请稍后再试");
        }

        // Generate 6-digit code
        String code = RandomUtil.randomNumbers(6);
        String key = RedisConstants.VERIFY_CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(key, code, RedisConstants.VERIFY_CODE_EXPIRE, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(rateKey, "1", 60, TimeUnit.SECONDS);

        log.info("SMS code for {}: {}", phone, code);
        // TODO: integrate with actual SMS service (Aliyun SMS / Tencent SMS)
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        String phone = request.getPhone();

        // Check if already registered
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (existing != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        // Verify SMS code
        String key = RedisConstants.VERIFY_CODE_PREFIX + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(key);
        if (cachedCode == null || !cachedCode.equals(request.getCode())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
        stringRedisTemplate.delete(key);

        // Create user
        User user = new User();
        user.setUserId(snowflakeIdGenerator.nextId());
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setUserName(request.getUserName() != null ? request.getUserName() : "用户" + phone.substring(phone.length() - 4));
        user.setAvatar("http://118.25.77.201:9000/infinitec-chat/infinitechat_default_avatar.png");
        user.setGender(2);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        return user.getUserId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账户已被封禁或注销");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = JwtUtil.generateToken(user.getUserId());

        // Store token in Redis for validation (e.g., logout blacklist check)
        stringRedisTemplate.opsForValue().set(
                RedisConstants.USER_TOKEN_PREFIX + user.getUserId(),
                token,
                RedisConstants.TOKEN_EXPIRE,
                TimeUnit.SECONDS);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toDTO(user));
        return response;
    }

    @Override
    public LoginResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LoginResponse response = new LoginResponse();
        response.setUser(toDTO(user));
        return response;
    }

    @Override
    public void logout(Long userId) {
        // Remove stored token (prevents new connections with this token)
        stringRedisTemplate.delete(RedisConstants.USER_TOKEN_PREFIX + userId);
        // Mark user as offline
        stringRedisTemplate.delete(RedisConstants.USER_ONLINE_PREFIX + userId);
        log.info("User {} logged out", userId);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setSignature(user.getSignature());
        dto.setGender(user.getGender());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
