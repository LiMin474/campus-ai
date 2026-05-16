package com.campus.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewAppealCreateRequest {
    @NotNull
    private Long reviewId;
    @NotBlank
    private String reason;
    private String evidenceUrl;
}