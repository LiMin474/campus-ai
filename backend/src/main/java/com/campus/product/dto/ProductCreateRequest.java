package com.campus.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private BigDecimal price;
    @NotNull
    private Long categoryId;
    private String conditionLabel;
    private List<String> imageUrls;
    private List<AttachmentDto> attachments;

    @Data
    public static class AttachmentDto {
        private String fileUrl;
        private String fileName;
    }
}
