package com.campus.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("product_attachment")
public class ProductAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String fileUrl;
    private String fileName;
    private LocalDateTime createdAt;
}
