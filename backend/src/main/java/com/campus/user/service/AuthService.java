package com.campus.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.util.JwtUtil;
import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.LoginResponse;
import com.campus.user.dto.RegisterRequest;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    private static final int MAX_LOGIN_FAIL = 5;
    private static final Duration LOGIN_LOCK_TTL = Duration.ofMinutes(15);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public void register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getStudentNo, request.getStudentNo()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("学号已被注册");
        }
        User user = new User();
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        user.setCreditScore(100);
        user.setCarbonPoints(0);
        user.setAppealFailCount(0);
        user.setBanned(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount();
        String failKey = LOGIN_FAIL_PREFIX + account;

        // 登录限流：失败超 5 次锁定 15 分钟
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= MAX_LOGIN_FAIL) {
            throw new IllegalArgumentException("登录失败次数过多，请15分钟后再试");
        }

        logger.info("Login attempt: account={}, mode={}", account, request.getMode());
        String role = "admin".equalsIgnoreCase(request.getMode()) ? "ADMIN" : "STUDENT";

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getRole, role)
            .and(w -> w.eq(User::getStudentNo, account).or().eq(User::getPhone, account)));

        if (user == null) {
            recordLoginFailure(failKey, failCount);
            throw new IllegalArgumentException("账号或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFailure(failKey, failCount);
            throw new IllegalArgumentException("账号或密码错误");
        }

        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new IllegalArgumentException("账号已被封禁");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        logger.info("Login successful for user: id={}", user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getRole());
    }

    private void recordLoginFailure(String failKey, int currentCount) {
        redisTemplate.opsForValue().set(failKey, String.valueOf(currentCount + 1), LOGIN_LOCK_TTL);
    }
}
