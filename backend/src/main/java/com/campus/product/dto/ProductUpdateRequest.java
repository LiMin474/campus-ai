package com.campus.product.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductUpdateRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String conditionLabel;
    private List<String> imageUrls;
    private List<ProductCreateRequest.AttachmentDto> attachments;
}
