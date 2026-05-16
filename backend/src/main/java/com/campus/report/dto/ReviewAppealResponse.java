package com.campus.report.dto;

import com.campus.report.entity.ReviewAppeal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewAppealResponse {
    private Long id;
    private Long reviewId;
    private Long userId;
    private String reason;
    private String evidenceUrl;
    private String status;
    private String adminOpinion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewAppealResponse fromEntity(ReviewAppeal appeal) {
        ReviewAppealResponse response = new ReviewAppealResponse();
        response.setId(appeal.getId());
        response.setReviewId(appeal.getReviewId());
        response.setUserId(appeal.getUserId());
        response.setReason(appeal.getReason());
        response.setEvidenceUrl(appeal.getEvidenceUrl());
        response.setStatus(appeal.getStatus());
        response.setAdminOpinion(appeal.getAdminOpinion());
        response.setCreatedAt(appeal.getCreatedAt());
        response.setUpdatedAt(appeal.getUpdatedAt());
        return response;
    }
}