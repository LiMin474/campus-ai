package com.campus.admin.dto;

import com.campus.product.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 管理员商品列表响应DTO
 */
@Data
@Builder
public class AdminProductResponse {
    private Long id;
    private String title;
    private BigDecimal price;
    private String categoryName;
    private String status;
    private String sellerNickname;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;

    public static AdminProductResponse fromEntity(Product product, String categoryName, String sellerNickname) {
        return AdminProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .categoryName(categoryName)
                .status(product.getStatus())
                .sellerNickname(sellerNickname)
                .viewCount(product.getViewCount())
                .likeCount(product.getLikeCount())
                .createdAt(product.getCreatedAt())
                .build();
    }
}