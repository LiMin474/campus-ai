package com.campus.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductListItemResponse {
    private Long id;
    private String title;
    private BigDecimal price;
    private String conditionLabel;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerNickname;
    private String sellerCreditLevel;
    private Integer viewCount;
    private Integer likeCount;
    private String coverImage;
    private LocalDateTime createdAt;
    private String status;
}
