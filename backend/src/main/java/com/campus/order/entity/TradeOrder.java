package com.campus.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("trade_order")
public class TradeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private String status;
    private BigDecimal finalPrice;
    private String confirmToken;
    private LocalDateTime confirmTokenExpire;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
