package com.campus.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private String conditionLabel;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private List<AttachmentVo> attachments;
    private SellerBrief seller;

    @Data
    @Builder
    public static class SellerBrief {
        private Long id;
        private String nickname;
        private String creditLevel;
        private Integer creditScore;
    }

    @Data
    @Builder
    public static class AttachmentVo {
        private String fileUrl;
        private String fileName;
    }
}
