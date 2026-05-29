package com.campus.admin.controller;

import com.campus.admin.dto.AdminDashboardResponse;
import com.campus.admin.dto.AdminOrderResponse;
import com.campus.admin.dto.AdminPostResponse;
import com.campus.admin.dto.AdminProductResponse;
import com.campus.admin.dto.PageResponse;
import com.campus.admin.service.AdminService;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.user.entity.User;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        SecurityUtils.requireAdmin();
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(@RequestParam(required = false) String keyword) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(adminService.listUsers(keyword));
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<Void> ban(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        SecurityUtils.requireAdmin();
        Boolean banned = body.get("banned");
        adminService.setBanned(id, Boolean.TRUE.equals(banned));
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/credit")
    public ApiResponse<Void> credit(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SecurityUtils.requireAdmin();
        Integer score = body.get("score");
        if (score == null) {
            throw new IllegalArgumentException("缺少 score");
        }
        adminService.resetCredit(id, score);
        return ApiResponse.success();
    }

    // ========== 商品管理 ==========
    
    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductResponse>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(adminService.listProducts(page, size, keyword, status));
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        adminService.deleteProduct(id);
        return ApiResponse.success();
    }

    @PutMapping("/products/{id}/status")
    public ApiResponse<Void> updateProductStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SecurityUtils.requireAdmin();
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("缺少 status");
        }
        adminService.updateProductStatus(id, status);
        return ApiResponse.success();
    }

    // ========== 帖子管理 ==========
    
    @GetMapping("/posts")
    public ApiResponse<PageResponse<AdminPostResponse>> posts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(adminService.listPosts(page, size, keyword));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        SecurityUtils.requireAdmin();
        adminService.deletePost(id);
        return ApiResponse.success();
    }

    // ========== 订单管理 ==========
    
    @GetMapping("/orders")
    public ApiResponse<PageResponse<AdminOrderResponse>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(adminService.listOrders(page, size, status));
    }

    @PutMapping("/orders/{id}/status")
    public ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SecurityUtils.requireAdmin();
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("缺少 status");
        }
        adminService.updateOrderStatus(id, status);
        return ApiResponse.success();
    }
}