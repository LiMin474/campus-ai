package com.campus.admin.controller;

/**
 * 管理员管理模块控制器
 * 负责成员E：管理员管理模块
 */

import com.campus.admin.dto.AdminDashboardResponse;
import com.campus.admin.service.AdminService;
import com.campus.common.api.ApiResponse;
import com.campus.user.entity.User;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listUsers(keyword));
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<Void> ban(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean banned = body.get("banned");
        adminService.setBanned(id, Boolean.TRUE.equals(banned));
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/credit")
    public ApiResponse<Void> credit(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer score = body.get("score");
        if (score == null) {
            throw new IllegalArgumentException("缺少 score");
        }
        adminService.resetCredit(id, score);
        return ApiResponse.success();
    }
}
