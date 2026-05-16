package com.campus.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.user.dto.UserProfileResponse;
import com.campus.user.dto.UserUpdateRequest;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import com.campus.user.util.CreditRules;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return UserProfileResponse.builder()
            .id(user.getId())
            .studentNo(user.getStudentNo())
            .phone(user.getPhone())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .creditScore(user.getCreditScore())
            .creditLevel(CreditRules.levelLabel(user.getCreditScore()))
            .carbonPoints(user.getCarbonPoints())
            .build();
    }

    @Transactional
    public void updateProfile(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone().trim())
                .ne(User::getId, userId));
            if (dup != null && dup > 0) {
                throw new IllegalArgumentException("手机号已被使用");
            }
            user.setPhone(request.getPhone().trim());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public int signIn(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastSigninDate())) {
            return user.getCarbonPoints();
        }
        user.setLastSigninDate(today);
        user.setCarbonPoints(user.getCarbonPoints() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user.getCarbonPoints();
    }
}
