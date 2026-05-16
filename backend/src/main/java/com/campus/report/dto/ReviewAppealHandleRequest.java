package com.campus.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewAppealHandleRequest {
    @NotNull
    private Boolean approve;
    private String adminOpinion;
}