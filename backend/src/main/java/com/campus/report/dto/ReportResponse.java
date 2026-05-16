package com.campus.report.dto;

import com.campus.report.entity.Report;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String evidenceUrl;
    private String status;
    private String adminRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportResponse fromEntity(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporterId());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setReason(report.getReason());
        response.setEvidenceUrl(report.getEvidenceUrl());
        response.setStatus(report.getStatus());
        response.setAdminRemark(report.getAdminRemark());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        return response;
    }
}