package com.campus.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String studentNo;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer creditScore;
    private String creditLevel;
    private Integer carbonPoints;
}
