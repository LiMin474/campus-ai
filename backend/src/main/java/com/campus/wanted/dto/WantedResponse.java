package com.campus.wanted.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WantedResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private Long categoryId;
    private String categoryName;
    private Long userId;
    private String userNickname;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private String coverImage;
    private List<String> imageUrls;
}
