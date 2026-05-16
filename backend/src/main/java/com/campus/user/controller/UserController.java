package com.campus.user.controller;

/**
 * 用户中心控制器
 * 负责成员A：用户中心 + 订单评价
 */

import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.user.dto.UserProfileResponse;
import com.campus.user.dto.UserUpdateRequest;
import com.campus.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(userService.profile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        userService.updateProfile(userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/sign-in")
    public ApiResponse<Integer> signIn() {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(userService.signIn(userId));
    }
}
