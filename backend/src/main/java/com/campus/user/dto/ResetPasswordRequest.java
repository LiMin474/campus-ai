package com.campus.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String studentNo;

    @NotBlank
    private String email;

    @NotBlank
    private String verifyCode;

    @NotBlank
    private String newPassword;
}