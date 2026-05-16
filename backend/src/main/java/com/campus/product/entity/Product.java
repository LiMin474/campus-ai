package com.campus.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private String conditionLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
