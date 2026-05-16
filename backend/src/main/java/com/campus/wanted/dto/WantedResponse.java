package com.campus.wanted.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
}
