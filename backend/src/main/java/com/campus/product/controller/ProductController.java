package com.campus.product.controller;

/**
 * 商品交易控制器
 * 负责成员B：商品交易 + 求购专区
 */

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.product.dto.ProductCreateRequest;
import com.campus.product.dto.ProductDetailResponse;
import com.campus.product.dto.ProductListItemResponse;
import com.campus.product.dto.ProductUpdateRequest;
import com.campus.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<Page<ProductListItemResponse>> page(
        @RequestParam(required = false) Boolean mine,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        if (Boolean.TRUE.equals(mine)) {
            Long userId = SecurityUtils.requireUserId();
            return ApiResponse.success(productService.pageMineItems(userId, categoryId, keyword, sort, page, size));
        }
        return ApiResponse.success(productService.pageItems(categoryId, keyword, sort, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ProductCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(productService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        productService.update(userId, id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<Void> offShelf(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        productService.offShelf(userId, id);
        return ApiResponse.success();
    }
}
