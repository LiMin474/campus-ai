package com.campus.user.controller;

import com.campus.common.api.ApiResponse;
import com.campus.user.dto.EmailVerifyCodeRequest;
import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.LoginResponse;
import com.campus.user.dto.RegisterRequest;
import com.campus.user.dto.ResetPasswordRequest;
import com.campus.user.service.AuthService;
import com.campus.user.service.EmailCodeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
    }

    @PostMapping("/auth/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }

    @GetMapping("/auth/send-register-code")
    public ApiResponse<String> sendRegisterCode(@Valid EmailVerifyCodeRequest request) {
        String error = emailCodeService.sendRegisterCode(request);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("验证码已发送至邮箱");
    }

    @PostMapping("/auth/complete-register")
    public ApiResponse<String> completeRegister(
            @RequestParam String studentNo,
            @RequestParam String email,
            @RequestParam String verifyCode,
            @RequestParam String nickname,
            @RequestParam String password
    ) {
        String error = emailCodeService.completeRegister(studentNo, email, verifyCode, nickname, password);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("注册成功");
    }

    @GetMapping("/auth/send-reset-pwd-code")
    public ApiResponse<String> sendResetPwdCode(@Valid EmailVerifyCodeRequest request) {
        String error = emailCodeService.sendResetPwdCode(request);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("验证码已发送至邮箱");
    }

    @GetMapping("/auth/verify-reset-pwd-code")
    public ApiResponse<String> verifyResetPwdCode(
            @RequestParam String studentNo,
            @RequestParam String email,
            @RequestParam String verifyCode) {
        String error = emailCodeService.verifyResetPwdCode(studentNo, email, verifyCode);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("验证成功");
    }

    @PostMapping("/auth/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String error = emailCodeService.resetPassword(request);
        if (error != null) {
            return ApiResponse.fail(error);
        }
        return ApiResponse.success("密码重置成功");
    }
}