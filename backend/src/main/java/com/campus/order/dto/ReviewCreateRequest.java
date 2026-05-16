package com.campus.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Long toUserId;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer communicationScore;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer matchScore;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer speedScore;
    private String content;
}
