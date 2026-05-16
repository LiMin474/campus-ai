package com.campus.report.controller;

import com.campus.common.api.ApiResponse;
import com.campus.common.util.SecurityUtils;
import com.campus.report.dto.ReportCreateRequest;
import com.campus.report.dto.ReportHandleRequest;
import com.campus.report.dto.ReportResponse;
import com.campus.report.dto.ReviewAppealCreateRequest;
import com.campus.report.dto.ReviewAppealHandleRequest;
import com.campus.report.dto.ReviewAppealResponse;
import com.campus.report.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ReportCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(reportService.submit(userId, request));
    }

    @PostMapping("/appeals")
    public ApiResponse<Long> createAppeal(@Valid @RequestBody ReviewAppealCreateRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.success(reportService.submitReviewAppeal(userId, request));
    }

    @GetMapping("/admin/pending")
    public ApiResponse<List<ReportResponse>> listPendingReports() {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(reportService.listPendingReports().stream()
            .map(ReportResponse::fromEntity)
            .collect(Collectors.toList()));
    }

    @GetMapping("/admin/all")
    public ApiResponse<List<ReportResponse>> listAllReports() {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(reportService.listAllReports().stream()
            .map(ReportResponse::fromEntity)
            .collect(Collectors.toList()));
    }

    @PostMapping("/admin/{id}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest request) {
        SecurityUtils.requireAdmin();
        reportService.handleReport(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/appeals/admin/pending")
    public ApiResponse<List<ReviewAppealResponse>> listPendingAppeals() {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(reportService.listPendingAppeals().stream()
            .map(ReviewAppealResponse::fromEntity)
            .collect(Collectors.toList()));
    }

    @GetMapping("/appeals/admin/all")
    public ApiResponse<List<ReviewAppealResponse>> listAllAppeals() {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(reportService.listAllAppeals().stream()
            .map(ReviewAppealResponse::fromEntity)
            .collect(Collectors.toList()));
    }

    @PostMapping("/appeals/admin/{id}/handle")
    public ApiResponse<Void> handleAppeal(@PathVariable Long id, @Valid @RequestBody ReviewAppealHandleRequest request) {
        SecurityUtils.requireAdmin();
        reportService.handleReviewAppeal(id, request);
        return ApiResponse.success();
    }
}
