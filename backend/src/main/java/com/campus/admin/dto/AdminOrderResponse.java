package com.campus.admin.dto;

import com.campus.order.entity.TradeOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 管理员订单列表响应DTO
 */
@Data
@Builder
public class AdminOrderResponse {
    private Long id;
    private Long productId;
    private String productTitle;
    private String buyerNickname;
    private String sellerNickname;
    private String status;
    private BigDecimal finalPrice;
    private LocalDateTime createdAt;

    public static AdminOrderResponse fromEntity(TradeOrder order, String productTitle, 
                                               String buyerNickname, String sellerNickname) {
        return AdminOrderResponse.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .productTitle(productTitle)
                .buyerNickname(buyerNickname)
                .sellerNickname(sellerNickname)
                .status(order.getStatus())
                .finalPrice(order.getFinalPrice())
                .createdAt(order.getCreatedAt())
                .build();
    }
}