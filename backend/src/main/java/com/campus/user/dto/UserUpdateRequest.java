package com.campus.user.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String phone;
    private String password;
    private String avatarUrl;
}
