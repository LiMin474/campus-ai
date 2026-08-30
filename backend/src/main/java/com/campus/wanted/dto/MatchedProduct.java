package com.campus.wanted.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/** 求购匹配到的商品（阶段一 · 被动匹配）。 */
@Data
@Builder
public class MatchedProduct {
    private Long productId;
    private String title;
    private BigDecimal price;
    private String coverImage;
}