package com.campus.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStudentNo, "admin"));
        if (exists != null && exists > 0) {
            return;
        }
        User admin = new User();
        admin.setStudentNo("admin");
        admin.setPhone("10000000000");
        admin.setNickname("系统管理员");
        admin.setPassword(passwordEncoder.encode("Admin123456"));
        admin.setRole("ADMIN");
        admin.setCreditScore(100);
        admin.setCarbonPoints(0);
        admin.setAppealFailCount(0);
        admin.setBanned(false);
        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userMapper.insert(admin);
    }
}
