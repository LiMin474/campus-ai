package com.campus.wanted.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class WantedRequest {
    @NotBlank
    private String title;
    private String description;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    @NotNull
    private Long categoryId;
    private List<String> imageUrls;
}
