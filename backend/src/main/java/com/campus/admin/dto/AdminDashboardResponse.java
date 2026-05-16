package com.campus.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardResponse {
    private Long userCount;
    private Long productCount;
    private Long pendingReportCount;
    private Long orderCount;
    private Long postCount;
    private BigDecimal totalSales;
    private List<DailyStat> dailyStats;
    private List<CategoryStat> categoryStats;
    private List<StatusStat> orderStatusStats;
    private List<StatusStat> productStatusStats;

    @Data
    @Builder
    public static class DailyStat {
        private LocalDate date;
        private Long userCount;
        private Long productCount;
        private Long orderCount;
        private BigDecimal salesAmount;
    }

    @Data
    @Builder
    public static class CategoryStat {
        private String categoryName;
        private Long productCount;
        private BigDecimal salesAmount;
    }

    @Data
    @Builder
    public static class StatusStat {
        private String status;
        private Long count;
    }
}
