package com.campus.wanted.controller;

/**
 * 求购专区控制器
 * 负责成员B：商品交易 + 求购专区
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.wanted.dto.MatchedProduct;
import com.campus.wanted.dto.WantedRequest;
import com.campus.wanted.dto.WantedResponse;
import com.campus.wanted.service.WantedService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wanted")
public class WantedController {

    private final WantedService wantedService;

    public WantedController(WantedService wantedService) {
        this.wantedService = wantedService;
    }

    @GetMapping
    public ApiResponse<Page<WantedResponse>> page(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(wantedService.page(categoryId, keyword, sort, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<Page<WantedResponse>> mine(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(wantedService.pageMine(userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<WantedResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(wantedService.detail(id));
    }

    @GetMapping("/{id}/matches")
    public ApiResponse<List<MatchedProduct>> matches(
        @PathVariable Long id,
        @RequestParam(defaultValue = "3") int topK
    ) {
        return ApiResponse.success(wantedService.matchProducts(id, topK));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody WantedRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(wantedService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody WantedRequest request) {
        Long userId = SecurityUtils.requireUserId();
        wantedService.update(userId, id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        wantedService.close(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<Void> reopen(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        wantedService.reopen(userId, id);
        return ApiResponse.success();
    }
}
