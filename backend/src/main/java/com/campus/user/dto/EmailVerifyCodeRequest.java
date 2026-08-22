package com.campus.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerifyCodeRequest {
    @NotBlank
    private String studentNo;

    @NotBlank
    private String email;
}