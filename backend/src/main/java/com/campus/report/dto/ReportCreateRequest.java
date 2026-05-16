package com.campus.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportCreateRequest {
    @NotBlank
    private String targetType;
    @NotNull
    private Long targetId;
    @NotBlank
    private String reason;
    private String evidenceUrl;
}
