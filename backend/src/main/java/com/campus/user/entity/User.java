package com.campus.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String phone;
    private String nickname;
    private String password;
    private String role;
    private Integer creditScore;
    private Integer carbonPoints;
    private Integer appealFailCount;
    private Boolean banned;
    private String avatarUrl;
    private LocalDate lastSigninDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
