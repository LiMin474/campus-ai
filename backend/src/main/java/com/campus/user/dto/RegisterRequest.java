package com.campus.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String studentNo;
    @NotBlank
    private String phone;
    @NotBlank
    private String nickname;
    @NotBlank
    private String password;
}
