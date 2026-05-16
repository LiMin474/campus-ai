package com.campus.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.report.dto.ReportCreateRequest;
import com.campus.report.dto.ReportHandleRequest;
import com.campus.report.dto.ReviewAppealCreateRequest;
import com.campus.report.dto.ReviewAppealHandleRequest;
import com.campus.report.entity.Report;
import com.campus.report.entity.ReviewAppeal;
import com.campus.report.mapper.ReportMapper;
import com.campus.report.mapper.ReviewAppealMapper;
import com.campus.user.entity.User;
import com.campus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final ReportMapper reportMapper;
    private final ReviewAppealMapper reviewAppealMapper;
    private final UserMapper userMapper;

    public ReportService(ReportMapper reportMapper, ReviewAppealMapper reviewAppealMapper, UserMapper userMapper) {
        this.reportMapper = reportMapper;
        this.reviewAppealMapper = reviewAppealMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Long submit(Long reporterId, ReportCreateRequest request) {
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(request.getTargetType().trim().toUpperCase());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setEvidenceUrl(request.getEvidenceUrl());
        report.setStatus(STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        reportMapper.insert(report);
        return report.getId();
    }

    @Transactional
    public Long submitReviewAppeal(Long userId, ReviewAppealCreateRequest request) {
        ReviewAppeal appeal = new ReviewAppeal();
        appeal.setReviewId(request.getReviewId());
        appeal.setUserId(userId);
        appeal.setReason(request.getReason().trim());
        appeal.setEvidenceUrl(request.getEvidenceUrl());
        appeal.setStatus(STATUS_PENDING);
        LocalDateTime now = LocalDateTime.now();
        appeal.setCreatedAt(now);
        appeal.setUpdatedAt(now);
        reviewAppealMapper.insert(appeal);
        return appeal.getId();
    }

    @Transactional
    public void handleReport(Long reportId, ReportHandleRequest request) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("举报不存在");
        }
        if (!STATUS_PENDING.equals(report.getStatus())) {
            throw new IllegalArgumentException("举报已处理");
        }

        report.setStatus(request.getApprove() ? STATUS_APPROVED : STATUS_REJECTED);
        report.setAdminRemark(request.getAdminRemark());
        report.setUpdatedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        // 如果举报被批准，可以根据 targetType 和 targetId 进行相应处理
        // 例如，封禁用户、删除商品等
    }

    @Transactional
    public void handleReviewAppeal(Long appealId, ReviewAppealHandleRequest request) {
        ReviewAppeal appeal = reviewAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new IllegalArgumentException("申诉不存在");
        }
        if (!STATUS_PENDING.equals(appeal.getStatus())) {
            throw new IllegalArgumentException("申诉已处理");
        }

        appeal.setStatus(request.getApprove() ? STATUS_APPROVED : STATUS_REJECTED);
        appeal.setAdminOpinion(request.getAdminOpinion());
        appeal.setUpdatedAt(LocalDateTime.now());
        reviewAppealMapper.updateById(appeal);

        // 如果申诉被拒绝，增加用户的申诉失败次数
        if (!request.getApprove()) {
            User user = userMapper.selectById(appeal.getUserId());
            if (user != null) {
                user.setAppealFailCount(user.getAppealFailCount() + 1);
                userMapper.updateById(user);
            }
        }
    }

    public List<Report> listPendingReports() {
        return reportMapper.selectList(new LambdaQueryWrapper<Report>()
            .eq(Report::getStatus, STATUS_PENDING)
            .orderByDesc(Report::getCreatedAt));
    }

    public List<Report> listAllReports() {
        return reportMapper.selectList(new LambdaQueryWrapper<Report>()
            .orderByDesc(Report::getCreatedAt));
    }

    public List<ReviewAppeal> listPendingAppeals() {
        return reviewAppealMapper.selectList(new LambdaQueryWrapper<ReviewAppeal>()
            .eq(ReviewAppeal::getStatus, STATUS_PENDING)
            .orderByDesc(ReviewAppeal::getCreatedAt));
    }

    public List<ReviewAppeal> listAllAppeals() {
        return reviewAppealMapper.selectList(new LambdaQueryWrapper<ReviewAppeal>()
            .orderByDesc(ReviewAppeal::getCreatedAt));
    }
}
