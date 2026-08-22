package com.campus.order.controller;

/**
 * 订单评价控制器
 * 负责成员A：用户中心 + 订单评价
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.order.dto.ConfirmByTokenRequest;
import com.campus.order.dto.OrderConfirmTokenResponse;
import com.campus.order.dto.OrderCreateRequest;
import com.campus.order.dto.OrderResponse;
import com.campus.order.dto.ReviewCreateRequest;
import com.campus.order.dto.ReviewResponse;
import com.campus.order.service.OrderService;
import com.campus.order.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    public OrderController(OrderService orderService, ReviewService reviewService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody OrderCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(orderService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<Page<OrderResponse>> page(
        @RequestParam(defaultValue = "buyer") String role,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(orderService.page(userId, role, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(orderService.detail(userId, id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        orderService.cancel(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        orderService.confirm(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm-order")
    public ApiResponse<Void> confirmOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        orderService.confirmOrder(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Void> ship(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        orderService.markAsShipped(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm-token")
    public ApiResponse<OrderConfirmTokenResponse> confirmToken(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(orderService.generateConfirmToken(userId, id));
    }

    @PostMapping("/confirm-with-token")
    public ApiResponse<Void> confirmWithToken(@Valid @RequestBody ConfirmByTokenRequest request) {
        Long userId = SecurityUtils.requireUserId();
        orderService.confirmWithToken(userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/reviews")
    public ApiResponse<Void> review(@Valid @RequestBody ReviewCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        reviewService.create(userId, request);
        return ApiResponse.success();
    }

    @GetMapping("/reviews/received")
    public ApiResponse<Page<ReviewResponse>> receivedReviews(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(reviewService.listReceivedReviews(userId, page, size));
    }

    @GetMapping("/reviews/given")
    public ApiResponse<Page<ReviewResponse>> givenReviews(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(reviewService.listGivenReviews(userId, page, size));
    }
}
